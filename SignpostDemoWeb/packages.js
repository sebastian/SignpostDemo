(function() {
  packages = {

    // Lazily construct the package hierarchy from class names.
    root: function(classes) {
      var map = {};

      function find(name, data) {
        var node = map[name], i;
        if (!node) {
          node = map[name] = data || {name: name, owned: "", children: []};
          if (name.length) {
            node.parent = find(node.owned);
            node.parent.children.push(node);
            node.key = name.replace(/\./g, "_");
          }
        }
        return node;
      }

      classes.forEach(function(d) {
        find(d.name, d);
      });

      return map[""];
    },

    // Return a list of connects for the given array of nodes.
    connects: function(nodes) {
      var map = {},
          connects = [];

      // Compute a map from name to node.
      nodes.forEach(function(d) {
        map[d.name] = d;
      });

      // For each import, construct a link from the source to target node.
      nodes.forEach(function(d) {
        if (d.connects) d.connects.forEach(function(i) {
          if (map[i] && map[d.name])
            connects.push({source: map[d.name], target: map[i]});
          else
            console.log("Possible problem with: " + d.name + " or " + i);
        });
      });

      console.log(connects);
      return connects;
    }


  };
})();
