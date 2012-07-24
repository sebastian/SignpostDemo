Mac:

Install godi following steps described in http://godi.camlcity.org/godi/get_godi.html

Get http://github.com/mor1/ocaml-re/

While building the server, it failed even when GODI reported the packages as
installed. Installing them using Opam  - another package manager (version installed 0.3.1)- might work
Update homebrew, otherwise it can fail
$ brew update --debug
$ brew tap mirage/homebrew-ocaml
$ brew install opam

Then
$ opam init
$ opam remote -kind git -add mirage git://github.com/mirage/opam-repo-dev
$ opam remote -kind git -add mor1 git://github.com/mor1/opam-repo-dev
$ opam install signpostd

Note: it failed during installation when trying to install cryptokit, running:
$ opam install cryptokit 
solved the issue

In any case, the essential packages are:
$opam install dns
$opam install ocamlgraph
$opam install bitstring

If these are installed, then it should be fine.


Nevertheless, eval `opam config -env` doesn't seem to work on mac. 

After several attemps,
it looks like adding to ./bashrc the following lines: (NOTE, replace
accordingly) solves the issue

PATH=/Users/Narseo/.opam/system/bin:/usr/bin:/Applications/android-sdk-mac_x86/platform-tools/:/Applications/android-sdk-mac_x86/tools/:/bin:/usr/sbin:/sbin:/usr/local/bin:/usr/local/git/bin:/usr/texbin:/usr/X11/bin:/opt/local/bin:/Users/Narseo/.odb/bin:/Users/Narseo/.rvm/bin:/opt/godi/bin/:/opt/godi/sbin/; export PATH;
OCAML_TOPLEVEL_PATH=/Users/Narseo/.opam/system/lib/toplevel; export OCAML_TOPLEVEL_PATH;
CAML_LD_LIBRARY_PATH=/Users/Narseo/.opam/system/lib/stublibs:/opt/godi/lib/ocaml/std-lib/stublibs; export CAML_LD_LIBRARY_PATH;





