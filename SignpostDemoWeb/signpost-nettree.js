var w = 800,
    h = 600,
    i = 0,
    duration = 500,
    root;

getSize();
w = w/2;

var tree = d3.layout.tree()
    .size([h, w]);

var diagonal = d3.svg.diagonal()
    .projection(function(d) { return [d.y, d.x]; });

var vis = d3.select("#chart").append("svg:svg")
    .attr("width", w+100)
    .attr("height", h)
  .append("svg:g");

d3.json("signpost-sigcomm-setup.json", function(json) {
  update(root = json);
});

// Cubism context
// TODO: adjust delays, steps and size to appropriate values

var divTime = d3.select("#chart").append("div")
    .style("top", "0px")
    .style("left", (w+120) + "px")
    .style("width", w/2 + "px")
    .style("height", h + "px")
    .style("position", "absolute");


var timeSeries = divTime.append("div")
  .attr("id", "time-series");

var context = cubism.context()
  .serverDelay(30 * 1000)   // Allow 30 seconds collection delay
  .step(15 * 1000)                // 15 seconds per value
  .size(300);


var bandwidth = [],
    latency = [],
    jitter = [];

function update(source) {

  // Compute the new tree layout.
  var nodes = tree.nodes(root).reverse();
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
    //.attr("transform", function(d) { return "translate(" + -10 + "," + -10 + ")"; })
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

  //TODO: 'nodes' contains position coodinates - collect the actual tunnel
  //data and draw links betweent the different positions
  //nodes[i].x, nodes[i].y, nodes[i].name


  
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
  

  nodeEnter.each(function(d) {
      bandwidth[d.name] = random("bandwidth", 100);
      latency[d.name] = random("latency", 10);
      jitter[d.name] = random("jitter", 100);
    });

  //TODO: connection stats for each node (can draw all together, as only
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
      .attr("style", "position: absolute; top: "+(node.x-minDX/3)+"px;")
      .call(function(div) {
        if (node.name == topClient.name){
          div.attr("style", "position: absolute; top: "+(node.x-minDX/3-30)+"px;")
          div.append("div")
            .attr("class", "axis")
            .call(context.axis().orient("top").tickSize(6).ticks(d3.time.minutes, 15));
        }

        div.selectAll(".horizon")
          .data(function(d) {
            return [bandwidth[node.name], latency[node.name], jitter[node.name]];
          })
          .enter().append("div")
            .attr("class", "horizon")
            .call(context.horizon().height(minDX/4));


        div.append("div")
          .attr("class", "rule")
          .call(context.rule());
      });
    });


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

