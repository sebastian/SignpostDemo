# Signpost Demo website

This website is supposed to visually display the behaviour of the Signpost
network and allow to control it.

Currently implemented interaction events are:
* Clicking on a device group owner it gets vertically centered at the left side
* Mouse over a device active connections are shown (red: current node is the
  source, green: it is the target)
* Clicking on a device, circle transitions into a semi-circle with only the
  relevant connections
* Also in that case connection "statistics" are displayed as time series, for
  each device (Stats are currently randomly generated)  

Dependencies:
-------------
* D3.js
* Cubism.js  

Data is read from a local, static json file

Server-side setup:
------------------
Server side for client connection stats is based on cube.js (http://square.github.com/cube/)

Cube.js dependencies:
* MongoDB
* MongoDB-Server
* Nodejs

Installation of Nodejs:
(For Debian wheezy, Debian Sid has nodejs available in the official repo):
    apt-get install make python g++
    mkdir ~/nodejs && cd $_
    wget -N http://nodejs.org/dist/node-latest.tar.gz
    tar xzvf node-latest.tar.gz && cd `ls -rd node-v*`
    make install

Installation of Cube:
    npm install cube
    # OR: git clone https://github.com/square/cube.git
    cd cube
    npm install


