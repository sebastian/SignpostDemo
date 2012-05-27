// TODO: refactor a lot - spaghetti now, mixing bundle layout, cubism, etc


/**
 * Initialize variables - screen size, graph coordinates
 */
var w, h;

getSize();

var ry = h / 2,
    rx = ry,
    m0,
    rotate = 0;

var splines = [];

var cluster = d3.layout.cluster()
    .size([360, ry-120])
    .sort(function(a, b) { return d3.ascending(a.key, b.key); });

cluster.separation( function separation(a, b) {
    return (a.parent == b.parent ? 1 : 4) / a.depth;
  });

var bundle = d3.layout.bundle();

var line = d3.svg.line.radial()
    .interpolate("bundle")
    .tension(.85)
    .radius(function(d) { return d.y; })
    .angle(function(d) { return d.x / 180 * Math.PI; });


/**
 * Prepare document's DOM for SVG drawing
 */ 
// Chrome 15 bug: <http://code.google.com/p/chromium/issues/detail?id=98951>
var div = d3.select("body").insert("div", "h2")
    .style("top", "-80px")
    .style("left", "-160px")
    .style("width", h + "px")
    .style("height", h + "px")
    .style("position", "absolute");

var svg = div.append("svg:svg")
    .attr("width", w)
    .attr("height", w)
  .append("svg:g")
    .attr("transform", "translate(" + rx + "," + ry + ")");

var divTime = d3.select("body").insert("div")
    .style("top", "40px")
    .style("left", h/2 + "px")
    .style("width", w-h + "px")
    .style("height", h + "px")
    .style("position", "absolute");


var timeSeries = divTime.append("div")
  .attr("id", "time-series");



svg.append("svg:path")
    .attr("class", "arc")
    .attr("d", d3.svg.arc().outerRadius(ry - 120).innerRadius(0).startAngle(0).endAngle(2 * Math.PI));
//    .on("mousedown", mousedown);


/**
 * Main drawing functions
 */ 
var gClasses;

function drawNodes(dNodes){
  //Draw graph nodes (devices)
  svg.selectAll("g.node")
    .data(dNodes.filter(function(n) { return !n.children; }), function(d) { return d.key; })
    .enter().append("svg:g")
    .attr("class", "node")
    .attr("id", function(d) { return "node-" + d.key; })
    .attr("transform", function(d) { return "rotate(" + (d.x - 90) + ")translate(" + d.y + ")"; })
    .append("svg:text")
    .attr("dx", function(d) { return d.x < 180 ? 8 : -8; })
    .attr("dy", ".31em")
    .attr("text-anchor", function(d) { return d.x < 180 ? "start" : "end"; })
    .attr("transform", function(d) { return d.x < 180 ? null : "rotate(180)"; })
    .text(function(d) { return d.name.replace("."+d.owned, ""); })
    .on("mouseover", nodemouseover)
    .on("mouseout", nodemouseout)
    .on("click", nodemouseclick);

  // Remove nodes that no longer exist
  svg.selectAll("g.node")
    .data(dNodes.filter(function(n) { return !n.children; }), function(d) { return d.key; })
    .exit()
    .remove();

  //Redraw existing nodes
  svg.selectAll("g.node")
    .data(dNodes.filter(function(n) { return !n.children; }), function(d) { return d.key; })
    .transition().duration(1000).delay(0).ease("cubic", 100)
    .attr("transform", function(d) { return "rotate(" + (d.x - 90) + ")translate(" + d.y + ")"; })
    .selectAll("text")
    .attr("dx", function(d) { return d.x < 180 ? 8 : -8; })
    .attr("dy", ".31em")
    .attr("text-anchor", function(d) { return d.x < 180 ? "start" : "end"; })
    .attr("transform", function(d) { return d.x < 180 ? null : "rotate(180)"; });
}

function drawNodeOwners(dNodes){
  // Draw node (cluster) owners
  var dataFilter = function(n) {
      if (n.children)
        return false;
      sameOwner = dNodes.filter( function (ni) {
        return ni.owned == n.owned;
      });
      sameOwner.sort(function(a, b) { return d3.ascending(a.key, b.key); });
      return n.key == sameOwner[Math.floor(sameOwner.length/2)].key;
  };

  var labelTransform = function(d) {
      return "rotate(" + (d.x - 90 )
      + ")translate(" + (d.y+80)
      + ")rotate(" + ( (d.x / 90) % 2 < 1 ? 93 : -93)+ ")";
  };
  svg.selectAll("g.node-owner")
    .data(dNodes.filter(dataFilter), function(d) { return d.key; })
  .enter().append("svg:g")
    .attr("class", "node-owner")
    .attr("id", function(d) { return "owner-" + d.owned; })
    .attr("transform", labelTransform)
  .append("svg:text")
    .attr("class", "node-owner")
    .attr("dy", ".31em")
    .attr("text-anchor", "middle")
    .attr("transform", function(d) { return d.x < 180 ? null : "rotate(180)"; })
    .text(function(d) { return d.owned; })
    .on("click", ownermouseclick);

  svg.selectAll("g.node-owner")
    .data(dNodes.filter(dataFilter), function(d) { return d.key; })
    .exit().remove();


  svg.selectAll("g.node-owner")
    .transition().duration(1000).delay(0).ease("cubic", 100)
    .attr("transform", labelTransform);
}

function drawLinks(dLinks, dSplines){
  // Draw the paths
  var svgPaths = svg.selectAll("path.link")
    .data(dLinks, function(d) { return d.source.key+d.target.key; });
  svgPaths
    .enter()
    .append("svg:path")
    .transition().duration(1000).delay(1000).ease("cubic", 100)
    .attr("class", function(d) { return "link source-" + d.source.key + " target-" + d.target.key; })
    .transition().duration(1000).delay(1000).ease("cubic", 100)
    .attr("d", function(d, i) { return line(dSplines[i]); });

  svg.selectAll("path.link")
    .data(dLinks, function(d) { return d.source.key+d.target.key; })
    .transition().duration(1000).delay(0).ease("cubic", 100)
    .attr("d", function(d, i) { return line(dSplines[i]); });

  svgPaths.exit().remove();
}

function drawBundle(dNodes, dLinks, dSplines){
  console.log("Drawing the bundle diagram");
  drawNodes(dNodes);
  drawNodeOwners(dNodes);
  drawLinks(dLinks, dSplines);
}

function rotateBundle(degrees){
  console.log("Rotate " + degrees);

  div.style("-webkit-transform", "rotate3d(0,0,0,0deg)");
  svg
    .transition().duration(1000).delay(1000).ease("cubic", 100)
      .attr("transform", "translate(" + rx + "," + ry + ")rotate(" + degrees + ")")
      .selectAll("g.node text")
      .attr("dx", function(d) { return (d.x + degrees ) % 360 < 180 ? 8 : -8; })
      .attr("text-anchor", function(d) { return (d.x + degrees ) % 360 < 180 ? "start" : "end"; })
      .attr("transform", function(d) { return (d.x + degrees ) % 360 < 180 ? null : "rotate(180)"; });

}

function drawCluster(dNodes){
  var diagonal = d3.svg.diagonal()
     .projection(function(d) { return [d.y, d.x]; });

  var dLinks = cluster.links(dNodes); 

  var link = svg.selectAll("path.link")
    .data(dLinks)
    .enter().append("path")
    .attr("class", "link")
    .attr("d", diagonal);

  var node = svg.selectAll("g.node")
    .data(dNodes)
    .enter().append("g")
    .attr("class", "node")
    .attr("transform", function (d) { return "translate(" + d.y + "," + d.x + ")";});

  node.append("text")
    .attr("dx", function(d) { return d.children ? -8 : 8; })
    .attr("dy", 3)
    .attr("text-anchor", function(d) { return d.children ? "end" : "start"; })
    .text(function(d) { return d.name; });
}

/**
 * Read in json data and trigger drawing
 */ 
d3.json("signpost-connections.json", function(classes) {
  gClasses = classes;
  var lNodes = cluster.nodes(packages.root(classes));
  var lLinks = packages.connects(lNodes);
  var lSplines = bundle(lLinks);

  drawBundle(lNodes, lLinks, lSplines);  
});

//d3.select(window)
//  .on("mousemove", mousemove)
//  .on("mouseup", mouseup);

function mouse(e) {
  return [e.pageX - rx, e.pageY - ry];
}

/**
 * Moving the circle around
 */
//function mousedown() {
//  m0 = mouse(d3.event);
//  d3.event.preventDefault();
//}

//function mousemove() {
//  if (m0) {
//    var m1 = mouse(d3.event),
//      dm = Math.atan2(cross(m0, m1), dot(m0, m1)) * 180 / Math.PI;
//    div.style("-webkit-transform", "translate3d(0," + (ry - rx) + "px,0)rotate3d(0,0,0," + dm + "deg)translate3d(0," + (rx - ry) + "px,0)");
//  }
//}
//
//function mouseup() {
//  if (m0) {
//    var m1 = mouse(d3.event),
//      dm = Math.atan2(cross(m0, m1), dot(m0, m1)) * 180 / Math.PI;
//
//    rotate += dm;
//    if (rotate > 360) rotate -= 360;
//    else if (rotate < 0) rotate += 360;
//    m0 = null;
//    
//    div.style("-webkit-transform", "rotate3d(0,0,0,0deg)");
//
//    svg
//      .attr("transform", "translate(" + rx + "," + ry + ")rotate(" + rotate + ")")
//      .selectAll("g.node text")
//      .attr("dx", function(d) { return (d.x + rotate) % 360 < 180 ? 8 : -8; })
//      .attr("text-anchor", function(d) { return (d.x + rotate) % 360 < 180 ? "start" : "end"; })
//      .attr("transform", function(d) { return (d.x + rotate) % 360 < 180 ? null : "rotate(180)"; });
//  }
//}

/**
 * Showing active lines
 */ 
function nodemouseover(d) {

  svg.selectAll("path.link.target-" + d.key)
    .classed("target", true)
    .each(updateNodes("source", true));

  svg.selectAll("path.link.source-" + d.key)
      .classed("source", true)
      .each(updateNodes("target", true));
}

function nodemouseout(d) {
  svg.selectAll("path.link.source-" + d.key)
      .classed("source", false)
      .each(updateNodes("target", false));

  svg.selectAll("path.link.target-" + d.key)
      .classed("target", false)
      .each(updateNodes("source", false));
}

function updateNodes(name, value) {
  return function(d) {
    if (value) this.parentNode.appendChild(this);
    svg.select("#node-" + d[name].key).classed(name, value);
  };
}


/**
 * Zoomed view of a particular node
 */ 
var zoomedNode = null;
var fromPosition = null;

function nodemouseclick(d) {
  console.log("Mouse clicked on label");
  if (!zoomedNode /* || zoomedNode != d.key*/){
    console.log("zooming in!");
    zoomedNode = d.key;
    fromPosition = mouse(d3.event);
    var zoomedDevices = gClasses.filter(function (n) {
      return d.connects.indexOf(n.name) > -1 || n.connects.indexOf(d.name) > -1 || n.key == d.key;
    });
    cluster.size([220, ry - 120]);
    var zoomedNodes = cluster.nodes(packages.root(zoomedDevices));
    var zoomedLinks = packages.connects(zoomedNodes);
    var zoomedSplines = bundle(zoomedLinks);


    drawBundle(zoomedNodes, zoomedLinks, zoomedSplines);
    rotate = 0;
    rotateBundle(160);
    nodemouseover(d);
    
    drawStats(d, zoomedNodes);
  }
  else{
    console.log("zooming out!");
    zoomedNode = null;
    timeSeries.selectAll("div")
      .transition().duration(500).delay(0).ease("cubic", 100)
      .style("display", "hidden")
      .remove();
    cluster.size([360, ry - 120]);
    var lNodes = cluster.nodes(packages.root(gClasses));
    var lLinks = packages.connects(lNodes);
    var lSplines = bundle(lLinks);
    drawBundle(lNodes, lLinks, lSplines);  

    currentPosition = mouse(d3.event);
    var tempNode = lNodes.filter(function (n) { return n.key == d.key; });
    var nodePosition = [tempNode[0].x, tempNode[0].y];
    console.log(currentPosition);
    console.log(nodePosition);
    dm = Math.atan2(cross(nodePosition, currentPosition), dot(nodePosition, currentPosition)) * 180 / Math.PI;
    rotate = 20;
    rotateBundle(rotate);

  }

}

function drawStats(source, targets){
      var bandwidth = [],
          latency = [],
          jitter = [];
      // TODO: more elegant solution for removal?
      timeSeries.selectAll("div").remove();
      if (targets){
        timeSeries.append("div")
          .text(source.name + " connection statistics: ");
      }
      for (node in targets){
        // Draw the time series only for the target nodes
        if (!targets[node].children && targets[node].key != source.key){
          // TODO: Statistics generation shouldn't be random..
          bandwidth[targets[node].key] = random("bandwidth", 100);
          latency[targets[node].key] = random("latency", 10);
          jitter[targets[node].key] = random("jitter", 100);

          timeSeries.append("div")
            .text("to " + targets[node].name)
            .attr("id", "time-series-"+source.key)
            .call(function(div) {
              div.append("div")
              .attr("class", "axis")
              .call(context.axis().orient("top"));

            div.selectAll(".horizon")
              .data([bandwidth[targets[node].key], latency[targets[node].key], jitter[targets[node].key]])
              .enter().append("div")
              .attr("class", "horizon")
              .call(context.horizon().extent([0, 700]));

            div.append("div")
              .attr("class", "rule")
              .call(context.rule());
            });
        }
      }
    }


function ownermouseclick(d) {
  console.log("Mouse clicked on owner");
  console.log(d);
  var ownerCoo = mouse(d3.event);
  var centerCoo = [-100, 0];
  dm = Math.atan2(cross(ownerCoo, centerCoo), dot(ownerCoo, centerCoo)) * 180 / Math.PI;
  if (Math.abs(dm) > 10){
  rotate += dm;
  if (rotate > 360) rotate -= 360;
  else if (rotate < 0) rotate += 360;
  rotateBundle(rotate);
  }
  else
    setTimeout(emphasizeOwner(d), 5000);
}

function emphasizeOwner(d) {
  console.log("Emphasizing owner");
  var ownerNodes = [];
  svg.selectAll("g.node")
    .filter(function (n, i) {
      return n.owned == d.owned;
    })
    .each(function (n, i) {
      ownerNodes[n.key] = {key: n.key, x: n.x, y: n.y};
    });
  cluster.size([360, ry - 120]);
  var lNodes = cluster.nodes(packages.root(gClasses));
  for (node in lNodes){
     if (ownerNodes[lNodes[node].key]){
        console.log(lNodes[node])
        console.log(ownerNodes[lNodes[node].key]);
        lNodes[node].x = ownerNodes[lNodes[node].key].x;
        lNodes[node].y = ownerNodes[lNodes[node].key].y;
     }
  }

  var lLinks = packages.connects(lNodes);
  var lSplines = bundle(lLinks);

  drawBundle(lNodes, lLinks, lSplines);

  d3.event.preventDefault();
}


/**
 * Utilities
 */ 

function cross(a, b) {
  return a[0] * b[1] - a[1] * b[0];
}

function dot(a, b) {
  return a[0] * b[0] + a[1] * b[1];
}


function getSize() {
  var myWidth = 0, myHeight = 0;
  if( typeof( window.innerWidth ) == 'number' ) {
    //Non-IE
    myWidth = window.innerWidth;
    myHeight = window.innerHeight;
  } else if( document.documentElement && ( document.documentElement.clientWidth || document.documentElement.clientHeight ) ) {
    //IE 6+ in 'standards compliant mode'
    myWidth = document.documentElement.clientWidth;
    myHeight = document.documentElement.clientHeight;
  } else if( document.body && ( document.body.clientWidth || document.body.clientHeight ) ) {
    //IE 4 compatible
    myWidth = document.body.clientWidth;
    myHeight = document.body.clientHeight;
  }
  w = myWidth;
  h = myHeight;
}

window.onresize = function(event) {
  console.log("Resized window");
  getSize();
  ry = h / 2;
  rx = ry;

  cluster.size()[1] = ry-120;
  svg.attr("transform", "translate(" + rx + "," + ry + ")");

  var lNodes = cluster.nodes(packages.root(gClasses));
  var lLinks = packages.connects(lNodes);
  var lSplines = bundle(lLinks);
  drawBundle(lNodes, lLinks, lSplines);  
}

// Cubism
//
function random(name, mult) {
  var value = 0,
      values = [],
      i = 0,
      last;
  return context.metric(function(start, stop, step, callback) {
    start = +start, stop = +stop;
    if (isNaN(last)) last = start;
    while (last < stop) {
      last += step;
      value = Math.abs(mult*Math.max(-10, Math.min(10, value/mult + .8 * Math.random() - .4 + .2 * Math.cos(i += .2))));
      values.push(value);
    }
    callback(null, values = values.slice((start - stop) / step));
  }, name);
}




var context = cubism.context()
    .serverDelay(0)
    .clientDelay(100)
    .step(1e3)
    .size(w-h);


