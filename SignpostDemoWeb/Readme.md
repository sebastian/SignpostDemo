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
