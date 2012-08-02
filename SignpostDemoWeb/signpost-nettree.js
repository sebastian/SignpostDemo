var w = 720,
    h = 900,
    i = 0,
    duration = 500,
    root;

var tree = d3.layout.tree()
    .size([h, w]);

var diagonal = d3.svg.diagonal()
    .projection(function(d) { return [d.y, d.x]; });

var tunLine = d3.svg.line()
    .x(function(d) { return d.x; })
    .y(function(d) { return d.y; })
    .interpolate("cardinal").tension(0.2);



/**
 * Prepare document's DOM for SVG drawing
 */ 
// Chrome 15 bug: <http://code.google.com/p/chromium/issues/detail?id=98951>
var mainDiv = d3.select("body").insert("div", "h2").insert("div").attr("id", "chart");

var vis = mainDiv.append("svg:svg")
    .attr("width", w+100)
    .attr("height", h)
  .append("svg:g");

var treeJson;

d3.json("signpost-sigcomm-setup.json", function(json) {
  treeJson = json;
  updateStructure(root = json);
});
d3.json("signpost-sigcomm-connections.json", function(tunnelsJson) {
  updateTunnels(root = tunnelsJson);
});

setInterval(function() { 
  d3.json("signpost-sigcomm-connections.json", function(json) {
    updateTunnels(root = json);
  });

  updateStructure(root = treeJson);
},10000);



// Time-series of node connection statistics

var timeSeriesDiv = mainDiv.append("div")
  .attr("id", "time-series")
  .style("top", "0px")
  .style("left", (w+120) + "px")
  .style("width", w+"px")
  .style("height", h+"px")
  .style("position", "absolute");



// Cubism context
// TODO: adjust delays, steps and size to appropriate values
var context = cubism.context()
  .serverDelay(1 * 1000)   // Allow 30 seconds collection delay
  .step(1e4)                // 10 seconds per value
  .size(500);

var cube = context.cube("http://ec2-107-20-107-204.compute-1.amazonaws.com:1081");

timeSeriesAxis = timeSeriesDiv
  .append("svg")
  .attr("class", "axis")
  .append("g")
  .attr("transform", "translate(0, 250)")
  .call(context.axis().orient("top")/*.tickSize(6).ticks(d3.time.minutes, 15)*/);

var nodes = null;

function updateStructure(source) {

  nodes = tree.nodes(root).reverse();
  console.log(nodes);

  // Update the nodes…
  var node = vis.selectAll("g.node")
      .data(nodes, function(d){ return d.name;} );

	var nodeEnter = node.enter().append("svg:g")
    	.attr("class", "node")
    	.attr("transform", function(d) { return "translate(" + source.y0 + "," + source.x0 + ")"; });
    	//.style("opacity", 1e-6);
 
  // Enter any new nodes at the parent's previous position.
 
  nodeEnter.append("svg:circle")
    //.attr("class", "node")
    //.attr("cx", function(d) { return source.x0; })
    //.attr("cy", function(d) { return source.y0; })
    .attr("r", 4.5)
    .style("fill", function(d) { return d._children ? "lightsteelblue" : "#fff"; })
    .on("click", click);
	
  nodeEnter.append("svg:text")
    .attr("x", function(d) { return d._children ? -8 : 8; })
		.attr("y", 3)
    .attr("fill", function(d) { return (d.type == "network") ? "#ccc" : "#000"; })
    .attr("transform", function(d) { return "translate(" + -6 + "," + -8 + ")"; })
    .text(function(d) { return d.name; });

  // Transition nodes to their new position.
  nodeEnter.transition()
    // .duration(duration)
		.attr("transform", function(d) { return "translate(" + d.y + "," + d.x + ")"; })
    .style("opacity", 1)
    .select("circle")
    //.attr("cx", function(d) { return d.x; })
		//.attr("cy", function(d) { return d.y; })
    .style("fill", "lightsteelblue");
      
  node.transition()
    // .duration(duration)
    .attr("transform", function(d) { return "translate(" + d.y + "," + d.x + ")"; })
    .style("opacity", 1);
    

	node.exit().transition()
    // .duration(duration)
    .attr("transform", function(d) { return "translate(" + source.y + "," + source.x + ")"; })
    .style("opacity", 1e-6)
    .remove();
  
  // Update the links…
  var link = vis.selectAll("path.link")
      .data(tree.links(nodes), function(d) { return d.source.name+"-"+d.target.name;});


  // Enter any new links at the parent's previous position.
  link.enter().insert("svg:path", "g")
    .attr("class", "link")
    .attr("d", function(d) {
      var o = {x: source.x, y: source.y};
      return diagonal({source: o, target: o});
    })
    .transition()
    // .duration(duration)
    .attr("d", diagonal);

  // Transition links to their new position.
  link.transition()
    // .duration(duration)
    .attr("d", diagonal);

  // Transition exiting nodes to the parent's new position.
  link.exit().transition()
    // .duration(duration)
    .attr("d", function(d) {
      var o = {x: source.x, y: source.y};
      return diagonal({source: o, target: o});
    })
    .remove();

  // Stash the old positions for transition.
  nodes.forEach(function(d) {
    d.x0 = d.x;
    d.y0 = d.y;
  });

  
  //connection stats for each node (can draw all together, as only
  //one possible node on the other side in the current setup)


  //Evaluate distance between child nodes to know the appropriate sizes of the
  //graphs
  var minDX = h;
  var minX = h;
  var topClient;
  node.filter(function(d){ return d.type == "client"; })
    .each(function(nodeA){
      node.filter(function(d2){ return d2.type == "client"; })
        .each(function(nodeB){
          if (nodeA.name != nodeB.name && Math.abs(nodeA.x - nodeB.x) < minDX)
            minDX = Math.abs(nodeA.x - nodeB.x);
          return minDX;
        })
      if (nodeA.x < minX){
        minX = nodeA.x;
        topClient = nodeA;
      }
      return minDX;
    });

  timeSeriesAxis.attr("transform", "translate(0, "+(minX-60)+")")

  // For all clients, add connection stats graph
  var timeSeriesData = timeSeriesDiv.selectAll(".time-series-class")
    .data(nodes.filter(function(d){ return d.type=="client"; }), 
            function(d){ return "time-series-"+d.name;}
         );


  var timeSeriesEnter = timeSeriesData.enter()
    .append("div")
    .attr("id", function(d){return "time-series-"+d.name;})
    .attr("class", "time-series-class")
    .attr("style", function(d){ 
      return "position: absolute; top: "+(d.x-minDX/3)+"px";
    });

  var metrics = ["bandwidth", "latency", "jitter"];
  var horizon = context.horizon().height(20);

  timeSeriesEnter
    .selectAll(".horizon")
    .data(metrics)
    .enter()
    .append("div")
    .attr("class", "horizon")
    .call(horizon
      .title(function(d) { return d; })
      .metric(function(d) { return cube.metric("median(stats("+d+").eq(node,'"+node.name+"'))"); })
    );
 
  
 timeSeriesEnter
    .append("div")
    .attr("class", "rule")
    .call(context.rule());

  timeSeriesData.transition()
    // .duration(duration)
    .attr("style", function(d){ 
      return "position: absolute; top: "+(d.x-minDX/3)+"px";
    });


  timeSeriesData
    .exit()
    .remove();

}

function updateTunnels(tunnels){
  //'nodes' contains position coodinates - collect the actual tunnel
  //data and draw links betweent the different positions
  //nodes[i].x, nodes[i].y, nodes[i].name

  pastClients = nodes.filter(function(d) { return d.type == "client"; });
  newClients = [];
  staleClients = [];
    
  tunnels.forEach(function (d){
    d.start = nodes.filter(function(nd){ return nd.name == d.client; })[0];
    d.end = nodes.filter(function(nd){ return nd.name == d.server; })[0];
    if (d.start != null && d.end != null){
      d.tunnelLine = [
        {y: d.start.x, x: d.start.y}, 
        {y: (d.start.x+d.end.x)/2, x: (d.start.y+d.start.y)/2+70}, 
        {y: d.end.x, x: d.end.y}
      ];
    }
    else
      d.tunneLine = [
        {y: d.end.x, x: d.end.y}
      ];

    // Only retrieving tunnels from the remote server, so is the client 
    // already in the network tree? If not, add it
    if (pastClients.filter(function(nd) { return nd.name == d.client; }).length == 0){
      newClients.push(d.client);
    }
  });

  // Filter out clients that are no longer there
  pastClients.forEach(function(d){
    if (tunnels.filter(function(td) { return td.client == d.name; }).length == 0){
      staleClients.push(d.name);
    }
  });

  treeJson = updateTreeJson(newClients, staleClients, treeJson);

  var tunnelData = vis.selectAll(".tunnel")
    .data(tunnels, function(d) { return d.client+"-"+d.server+"-"+d.type;});


  tunnelData
    .enter()
    .append("svg:path")
    .attr("class", function(d){ return "tunnel tunnel-"+d.type;} )
    .attr("d", function(d){ return tunLine(d.tunnelLine);} );

  tunnelData
    .transition()
    // .duration(duration)
    .attr("d", function(d){ return tunLine(d.tunnelLine);});

  tunnelData
    .exit()
    // .duration(duration)
    .remove();

}

// Toggle children on click.
function click(d) {
  if (d.children) {
    d._children = d.children;
    d.children = null;
  } else {
    d.children = d._children;
    d._children = null;
  }
  update(d);
}

// d3.select(self.frameElement).style("height", h+"px");

// TODO: very hacked-up version of updating the tree
// Assumes fixed structure apart from the clients connecting to a specific
// node, fixed client type
//
function updateTreeJson(newNodes, staleNodes, pastTree){
  homeNatNode = pastTree.children[0].children[1];

  for (i in newNodes){
    newNode = {
      name : newNodes[i],
      parent: homeNatNode,
      type: "client"
    };
    homeNatNode.children.push(newNode);
  }

  homeNatNode.children = homeNatNode.children.filter(function(d) {
    return staleNodes.indexOf(d.name) == -1;
  });

  console.log(homeNatNode);

  return pastTree;
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

