open Lwt
open Re_str

let bytes_per_megabit = 131072.0
let num_bytes = 1000000
let udp_listening_port = "57654"
let d = String.create num_bytes

let jitter_sender ~clientsocket ~client_id ~udp_port =
  let open Unix in
  let open Printf in
  let send_loop ip port =
    let hostname = "server" in
    let dst = Udp_server.addr_from ip port in
    while_lwt true do
      lwt _ = Lwt_unix.sleep (0.02) in
      let timestamp = Unix.gettimeofday () in
      let jitter = Store.get_jitter client_id in
      let payload = sprintf "%S\r\n%f\r\n%f\r\n" hostname timestamp jitter in
      Udp_server.send_datagram payload dst;
      return ()
    done
  in
  match clientsocket with
  | ADDR_UNIX sn ->
      printf "Connected with unix socket %S. We don't support that.\n%!" sn;
      return ()
  | ADDR_INET(inet_addr, _port) ->
      let ip = Unix.string_of_inet_addr inet_addr in
      printf "Client %S, IP: %S UDP port: %i\n%!" client_id ip udp_port;
      send_loop ip udp_port

let udp_handler ~content = 
  let open Re_str in
  let rgxp = regexp "\r\n" in
  match (split rgxp content) with
  | [hostname; timestamp; jitterFloat] ->
      let current_time = Unix.gettimeofday () in
      let timestamp_float = float_of_string timestamp in
      let jitter_diff_ms = (current_time -. timestamp_float) in
      Store.add_jitter_measurement hostname jitter_diff_ms;
      let jitter_seen_locally = Store.get_jitter hostname in
      return ()
  | _ ->
      Printf.printf "Malformed UDP jitter packet.\n%!";
      return ()

let tcp_handler ~clisockaddr ~srvsockaddr ic oc =
  let wl s = Lwt_io.write oc (s ^ "\r\n") in
  let rl () = Lwt_io.read_line ic in

  (* Handshake *)
  lwt id = rl () in
  lwt port = rl () in
  let port_int = int_of_string port in
  lwt () = wl udp_listening_port in
  jitter_sender ~clientsocket:clisockaddr ~client_id:id ~udp_port:port_int;

  Printf.printf "%S connected.\n%!" id;

  (* Allow to perform measurements repeatedly *)
  while_lwt true do
    (* Ping *)
    lwt _ping = rl () in

    (* Get start time for latency measurement *)
    let t1 = Unix.gettimeofday () in

    (* Tell client how many bytes to expect *)
    lwt () = wl (string_of_int num_bytes) in   

    (* Read latency seen by client in microseconds *)
    lwt clat_microseconds_str = rl () in

    (* Calculate latency seen by the server in ms *)
    let t2 = Unix.gettimeofday () in
    let clat_ms = (float_of_string clat_microseconds_str) /. 1000.0 in 
    let slat_ms = (t2 -. t1) *. 1000. /. 2. in

    (* Write data to client for downstream goodput measurement *)
    lwt () = Lwt_io.write oc d in

    (* Read downstream goodput *)
    lwt dstr_in_kb_int = rl () in
    let downstream_bandwidth_in_mb = (float_of_string dstr_in_kb_int) /. 1000. in

    (* Good place to store some values, since there is no
     * timing sensitive work happening at the moment *)
    Store.set_downstream_bandwidth id downstream_bandwidth_in_mb;
    Store.set_latency id clat_ms slat_ms;

    (* Tell the client what latency we see *)
    let slat_microseconds_int = int_of_float (slat_ms *. 1000.0) in
    let t3 = Unix.gettimeofday () in
    lwt () = wl (string_of_int slat_microseconds_int) in 

    (* Read data from client *)
    lwt _ = Lwt_io.read ~count:num_bytes ic in

    (* Calculate upstream goodput *)
    let t4 = Unix.gettimeofday () in
    let transmission_time_ms = (t4 -. t3) *. 1000.0 -. (slat_ms *. 2.) in
    let numPerSecond = (1000. /. transmission_time_ms) in
    let bytesPerSecond = numPerSecond *. (float_of_int num_bytes) in
    let upstream_mbitPerSecond = bytesPerSecond /. bytes_per_megabit in
    Store.set_upstream_bandwidth id upstream_mbitPerSecond;

    (* Report upstream goodput to client *)
    let upstream_kbitPerSecond_int = 
        int_of_float (upstream_mbitPerSecond *. 1000.) in
    wl (string_of_int upstream_kbitPerSecond_int)
  done

let main () =
  Lwt_engine.set (new Lwt_engine.select);
  let open Unix in
  Store.thread ();
  Udp_server.thread ~address:"0.0.0.0" ~port:(int_of_string udp_listening_port) udp_handler;
  Tcp_server.simple 
      ~sockaddr:(ADDR_INET (inet_addr_any, 7777)) 
      ~timeout:None
      tcp_handler

let _ = Lwt_unix.run (main ())
