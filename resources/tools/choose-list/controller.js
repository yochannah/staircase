define(['angular', 'imjs'], function (ng, im) {
  "use strict";

  var connect = im.Service.connect;

  var ChooseListCtrl = ['$scope', '$log', '$q', '$timeout', '$cacheFactory', 'Mines',
          function (scope, logger, Q, timeout, cacheFactory, mines) {

    scope.serviceName = "";
    scope.filters = {};

    var mineName = 'default';
    if (scope.tool.args && scope.tool.args.service) {
      mineName = scope.tool.args.service;
    }

    var fetchingDefaultMine = mines.get(mineName);

    fetchingDefaultMine.then(setMineDetails);
    
    fetchingDefaultMine.then(connect)
                       .then(readLists)
                       .then(null, function (e) { scope.tool.error = e; });
    
    scope.viewList = viewList;

    scope.$watch('serviceName', function (name) {
      if (name) scope.tool.heading = "Lists in " + name;
    });

    scope.$watch('filters.category', function (cat) {
      if (!cat) return;
      scope.lists = scope.allLists.filter(cat.matches.bind(cat));
    });

    function viewList (list) {
      scope.$emit('start-history', {
        verb: {
          ed: "chose",
          ing: "choosing"
        },
        thing: "list " + list.title,
        tool: "show-list",
        data: {
          service: {
            root: scope.connection.root
          },
          listName: list.name
        }
      });
    }

    function readLists (conn) {
      scope.connection = conn;
      var d = Q.defer();
      var fetching = conn.fetchLists();
      fetching.then(setLists);
      fetching.then(setTags);
      fetching.then(d.resolve, d.reject);
      timeout(function () {
        d.reject(new Error("List request timed out"));
      }, 30000); // 30sec is more than generous for list requests.
      return d.promise;
    }

    function setLists (lists) {
      timeout(function () {
        scope.lists = scope.allLists = lists.filter(listFilter);
        var n = scope.lists.length;
        if (n < 5) {
          scope.tool.tall = false;
        }
      });
    }

    function setTags (lists) {
      timeout(function () {
        var i, j, l, k, key, cat, types = {}, tags = {};
        for (i = 0, l = lists.length; i < l; i++) {
          key = lists[i].type;
          cat = types[key] || (types[key] = new TypeCategory(Q, lists[i].service, key));
          cat.size++;
          for (j = 0, k = lists[i].tags.length; j < k; j++) {
            key = lists[i].tags[j];
            cat = tags[key] || (tags[key] = new TagCategory(key));
            cat.size++;
          }
        }
        var all = new Category('ALL', lists.length);
        scope.categories = [all].concat(values(tags)).concat(values(types));
      });
    }

    function setMineDetails (mine) {
      timeout(function () { scope.serviceName = mine.name; });
    }

  }];

  function listFilter (list) {
    return list.size && list.status === "CURRENT";
  }

  function values (obj) {
    var key, vals = [];
    for (key in obj) {
      vals.push(obj[key]);
    }
    return vals;
  }

  function Category(name, size) {
    this.size = 0;
    if (size) this.size = size;
    this.name = name;
    this.matches = function () { return true; };
  }

  function TypeCategory(Q, connection, typeName) {
    var that = this;
    connection.fetchModel().then(function(model) {
      Q.when(model.makePath(typeName).getDisplayName()).then(function (name) {
        that.name = name + 's';
      });
    });

    this.matches = function (list) {
      return list.type === typeName;
    };
  }

  TypeCategory.prototype = new Category();

  function TagCategory(tagName) {
    this.name = tagName.replace(/^im:/, '');

    this.matches = function (list) {
      return list.tags.some(function (t) { return t === tagName; });
    };
  }

  TagCategory.prototype = new Category();

  return ChooseListCtrl;

});
