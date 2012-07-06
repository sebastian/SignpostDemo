# Signpost Demo applications

This repository contains demo applications for measuring
goodput, latency and jitter between two hosts for a demo
of Signpost.

For more information about signpost, please visit http://www.signpost.io

# submodules

in order to fetch the signpost engine code run:
git submodules init SignpostEngine
git submodule update SignpostEngine
cd SignpostEngine
./scripts/build-ocaml-deps
make
