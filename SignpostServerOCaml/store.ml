open Lwt

type data = {
  upstream_bw : float;
  downstream_bw : float;
  client_latency : float;
  server_latency : float;
  current_jitter : float;
  initial_timestamp: float;
  jitter_measurements : float list
}
type db = {
  clients : (string, data) Hashtbl.t
}

let jitter_messages_to_keep = 60

let db = {clients = Hashtbl.create 10}

let get client_id =
  try Hashtbl.find db.clients client_id
  with Not_found ->
    {
      upstream_bw = 0.;
      downstream_bw = 0.;
      client_latency = 0.;
      server_latency = 0.;
      current_jitter = 0.;
      initial_timestamp = 0.;
      jitter_measurements = []
    }

let store client_id record =
  Hashtbl.replace db.clients client_id record

let set_initial_timestamp client_id timestamp =
  let data = get client_id in
  store client_id {data with initial_timestamp = timestamp}

let set_upstream_bandwidth client_id bw = 
  let data = get client_id in
  store client_id {data with upstream_bw = bw}

let set_downstream_bandwidth client_id bw = 
  let data = get client_id in
  store client_id {data with downstream_bw = bw}

let set_latency client_id clat slat = 
  let data = get client_id in
  store client_id {data with client_latency = clat; server_latency = slat}

let add_jitter_measurement client_id measurement =
  let data = get client_id in
  let measurements = data.jitter_measurements in
  let filterfn el (acc, count) = 
    if count > (jitter_messages_to_keep - 1) then
      (acc, count)
    else
      (el :: acc, count + 1)
  in
  let (new_measurements, _) = 
      List.fold_right filterfn measurements ([measurement], 1) in
  store client_id {data with jitter_measurements = new_measurements}

let get_jitter client_id = 
  let data = get client_id in
  data.current_jitter

let get_initial_timestamp client_id =
  let data = get client_id in
  data.initial_timestamp 

let set_jitter client_id jitter = 
  let data = get client_id in
  store client_id {data with current_jitter = jitter}


(* Jitter calculations *)
let jitter jitter_list =
  let length = float_of_int(List.length jitter_list) in
  let sum = List.fold_left (fun acc item -> acc +. item ) 0. jitter_list in
  let mean = sum /. length in
  let foldfn ss el = 
    let difference = el -. mean in
    ss +. difference *. difference
  in
  let sumOfSquaredDifferences = List.fold_left foldfn 0. jitter_list in
  sumOfSquaredDifferences /. length

let update_jitter client_id data =
  let new_jitter = (jitter data.jitter_measurements *. 1000.) in
  set_jitter client_id new_jitter

let thread () =
  while_lwt true do
    (* Calculate the jitters every 200ms *)
    lwt _ = Lwt_unix.sleep (0.2) in
    Hashtbl.iter update_jitter db.clients;
    return ()
  done

