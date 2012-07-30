open Lwt

let stats_server_name = "ec2-107-20-107-204.compute-1.amazonaws.com" 
(* let stats_server_name = "107.20.107.204" *)
let stats_server_port = 1180

lwt stats_dst = try_lwt
  let hent = Unix.gethostbyname stats_server_name in
  return (Unix.ADDR_INET (hent.Unix.h_addr_list.(0), stats_server_port))
with _ ->
  raise_lwt (Failure ("cannot resolve " ^ stats_server_name))

let get_current_time_str =
  let timestamp = Unix.gmtime(Unix.time()) in
  let time_str = Printf.sprintf "%04d-%02d-%02dT%02d:%02d:%02dZ" (timestamp.Unix.tm_year + 1900 )
    (timestamp.Unix.tm_mon+1) timestamp.Unix.tm_mday timestamp.Unix.tm_hour
    timestamp.Unix.tm_min timestamp.Unix.tm_sec in
  time_str

let send_stats message =
  Printf.printf "%s\n" message

let stats_message client_id message_time dataField data = 
  let open Json in
  let message = Json.Array [ Json.Object
    [("type", Json.String "stats"); 
     ("time", Json.String message_time); 
     ("data", Json.Object 
       [("node", Json.String client_id);
        (dataField, Json.Float data)]
     )]] in
  let message_str = Json.to_string message in
  message_str

let send_downstream_bandwidth client_id bw =
  let open Json in
  let current_time = get_current_time_str in
  let message = stats_message client_id current_time "bandwidth" bw in
  send_stats message

let send_client_latency client_id latency = 
  let open Json in
  let current_time = get_current_time_str in
  let message = stats_message client_id current_time "latency" latency in
  send_stats message

let send_jitter client_id jitter = 
  let open Json in
  let current_time = get_current_time_str in
  let message = stats_message client_id current_time "jitter" jitter in
  send_stats message
