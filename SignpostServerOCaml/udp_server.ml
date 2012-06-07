open Lwt
open Printf

let fd = Lwt_unix.(socket PF_INET SOCK_DGRAM 0)

(* Listens on port Config.signal_port *)
let bind_fd ~address ~port =
  lwt src = try_lwt
    let hent = Unix.gethostbyname address in
    return (Unix.ADDR_INET (hent.Unix.h_addr_list.(0), port))
  with _ ->
    raise_lwt (Failure ("cannot resolve " ^ address))
  in
  let () = Lwt_unix.bind fd src in
  return fd

let sockaddr_to_string =
  function
  | Unix.ADDR_UNIX x -> sprintf "UNIX %s" x
  | Unix.ADDR_INET (a,p) -> sprintf "%s:%d" (Unix.string_of_inet_addr a) p

let thread ~address ~port callback =
  (* Listen for UDP packets *)
  lwt fd = bind_fd ~address ~port in
  while_lwt true do
    let buf = String.create 4096 in
    lwt len, dst = Lwt_unix.recvfrom fd buf 0 (String.length buf) [] in
    let subbuf = String.sub buf 0 len in
    callback ~content:subbuf;
    return ()
  done

let send_datagram text dst =
  Lwt_unix.sendto fd text 0 (String.length text) [] dst

let addr_from ip port = 
  Unix.(ADDR_INET (inet_addr_of_string ip, port))
