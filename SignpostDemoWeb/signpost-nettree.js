var w = 720,
    h = 900,
    i = 0,
    duration = 500,
    root;

var tree = d3.layout.tree()
    .size([h, w]);

var diagonal = d3.svg.diagonal()
    .projection(function(d) { return [d.y, d.x]; });

/**
 * Prepare document's DOM for SVG drawing
 */ 
// Chrome 15 bug: <http://code.google.com/p/chromium/issues/detail?id=98951>
var div = d3.select("body").insert("div", "h2").insert("div").attr("id", "chart");

var vis = div.append("svg:svg")
    .attr("width", w+100)
    .attr("height", h)
  .append("svg:g");

d3.json("signpost-sigcomm-setup.json", function(json) {
  updateStructure(root = json);
});

d3.json("signpost-sigcomm-connections.json", function(json) {
  updateTunnels(json);
});


// Time-series of node connection statistics

var timeSeries = div.append("div")
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
  .step(1e4)                // 15 seconds per value
  .size(500);

var cube = context.cube("http://ec2-107-20-107-204.compute-1.amazonaws.com:1081");

var nodes = null;

function updateStructure(source) {

  // Compute the new tree layout.
  nodes = tree.nodes(root).reverse();
  console.log(nodes)
  // Update the nodes…
  var node = vis.selectAll("g.node")
      .data(nodes, function(d) { return d.id || (d.id = ++i); });

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
    .duration(duration)
		.attr("transform", function(d) { return "translate(" + d.y + "," + d.x + ")"; })
    .style("opacity", 1)
    .select("circle")
    //.attr("cx", function(d) { return d.x; })
		//.attr("cy", function(d) { return d.y; })
    .style("fill", "lightsteelblue");
      
  node.transition()
    .duration(duration)
    .attr("transform", function(d) { return "translate(" + d.y + "," + d.x + ")"; })
    .style("opacity", 1);
    

	node.exit().transition()
    .duration(duration)
    .attr("transform", function(d) { return "translate(" + source.y + "," + source.x + ")"; })
    .style("opacity", 1e-6)
    .remove();
/*
	var nodeTransition = node.transition()
		.duration(duration);
  
  nodeTransition.select("circle")
      .attr("cx", function(d) { return d.y; })
      .attr("cy", function(d) { return d.x; })
      .style("fill", function(d) { return d._children ? "lightsteelblue" : "#fff"; });
  
  nodeTransition.select("text")
      .attr("dx", function(d) { return d._children ? -8 : 8; })
	  .attr("dy", 3)
      .style("fill", function(d) { return d._children ? "lightsteelblue" : "#5babfc"; });

  // Transition exiting nodes to the parent's new position.
  var nodeExit = node.exit();
  
  nodeExit.select("circle").transition()
      .duration(duration)
      .attr("cx", function(d) { return source.y; })
      .attr("cy", function(d) { return source.x; })
      .remove();
  
  nodeExit.select("text").transition()
      .duration(duration)
      .remove();
*/
  // Update the links…
  var link = vis.selectAll("path.link")
      .data(tree.links(nodes), function(d) { return d.target.id; });


  // Enter any new links at the parent's previous position.
  link.enter().insert("svg:path", "g")
    .attr("class", "link")
    .attr("d", function(d) {
      var o = {x: source.x0, y: source.y0};
      return diagonal({source: o, target: o});
    })
    .transition()
    .duration(duration)
    .attr("d", diagonal);

  // Transition links to their new position.
  link.transition()
    .duration(duration)
    .attr("d", diagonal);

  // Transition exiting nodes to the parent's new position.
  link.exit().transition()
    .duration(duration)
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

  
  
  // TODO: no random!
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
  console.log(minDX);

  // For all clients, add connection stats graph
  nodeEnter.filter(function(d) {
      return d.type == "client";
    })
    .each(function(node) {
      timeSeries.append("div")
      .attr("id", "time-series-"+ node.name)
      .call(function(div) {
          div.attr("style", "position: absolute; top: "+(node.x-minDX/3-30)+"px;")
          div.append("div")
            .attr("class", "axis")
            .call(context.axis().orient("top")/*.tickSize(6).ticks(d3.time.minutes, 15)*/);

        var metrics = ["bandwidth", "latency", "jitter"];

        div.selectAll(".horizon")
          .data(metrics)
          .enter().append("div")
            .attr("class", "horizon")
            .call(context.horizon()
                .height(20)        // Breaks the graph!
                .title(function(d) { return d; })
                .metric(function(d) { return cube.metric("median(stats("+d+").eq(node,'"+node.name+"'))"); })
            );


        div.append("div")
          .attr("class", "rule")
          .call(context.rule());
      });
    });


}

function updateTunnels(tunnels){
  //TODO: 'nodes' contains position coodinates - collect the actual tunnel
  //data and draw links betweent the different positions
  //nodes[i].x, nodes[i].y, nodes[i].name

  var line = d3.svg.line()
    .x(function(d) { return d.x; })
    .y(function(d) { return d.y; })
    .interpolate("cardinal").tension(0.2);

  vis.selectAll("tunnel")
    .data(tunnels.map(function(d){
      console.log(d);
      start = nodes.filter(function(nd){ return nd.name == d.client; })[0];
      end = nodes.filter(function(nd){ return nd.name == d.server; })[0];
      tunnelLine = [
                    {y: start.x, x: start.y}, 
                    {y: (start.x+end.x)/2, x: (start.y+start.y)/2+70}, 
                    {y: end.x, x: end.y}
      ];
      console.log(tunnelLine);

      return {line:tunnelLine, type:d.type};
    }
    ))
    .enter()
    .append("svg:path")
    .attr("class", function(d){ return "tunnel-"+d.type;} )
    .attr("d", function(d){ return line(d.line);} );

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

