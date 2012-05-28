open Lwt

let d = String.create 200

let callback ~clisockaddr ~srvsockaddr ic oc =
  Printf.printf "callback 1\n%!";
  let wl s = Printf.printf "W %S\n" s; Lwt_io.write oc (s ^ "\r\n") in
  let rl () =
    lwt x = Lwt_io.read_line ic in
    Printf.printf "R %S\n%!" x;
    return x
  in
  lwt id = rl () in
  lwt port = rl () in
  let port = int_of_string port in
  lwt () = wl "7000" in
  while_lwt true do
    lwt ping = rl () in
    Printf.printf "PING %S\n%!" ping;
    let num_bytes = 200 in
    let t1 = Unix.gettimeofday () in
    lwt () = wl (string_of_int num_bytes) in   
    lwt clat = rl () in
    let t2 = Unix.gettimeofday () in
    let clat = float_of_string clat in 
    let slat = (t2 -. t1) /. 2. in
    lwt () = wl d in
    lwt dstr = rl () in
    let t3 = Unix.gettimeofday () in
    lwt () = wl (string_of_float slat) in 
    lwt _ = Lwt_io.read ~count:num_bytes ic in
    let t4 = Unix.gettimeofday () in
    let slat2 = t4 -. t3 -. (slat *. 2.) in
    wl (string_of_float slat2)
  done

let main () =
  let open Unix in
  Tcp_server.simple ~sockaddr:(ADDR_INET (inet_addr_any, 7777)) ~timeout:None callback

let _ = Lwt_unix.run (main ())
