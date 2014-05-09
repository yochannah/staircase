define(['angular', 'imjs'], function (ng, im) {
  "use strict";

  var connect = im.Service.connect;

  return ['$scope', '$log', '$timeout', '$cacheFactory', 'Mines',
          function (scope, logger, timeout, cacheFactory, mines) {

    scope.lists = [];
    scope.serviceName = "";

    var fetchingDefaultMine = mines.get('default');

    fetchingDefaultMine.then(setMineDetails);
    
    fetchingDefaultMine.then(connect).then(readLists);
    
    scope.viewList = viewList;

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
            root: scope.connection.root,
            token: scope.connection.token,
          },
          listName: list.name
        }
      });
    }

    function readLists (conn) {
      scope.connection = conn;
      var fetching = conn.fetchLists();
      fetching.then(setLists);
      fetching.then(setTags);
    }

    function setLists (lists) {
      timeout(function () {
        scope.lists = lists.filter(listFilter);
      });
    }

    function setTags (lists) {
      timeout(function () {
        var i, j, l, k, key, cat, types = {}, tags = {};
        for (i = 0, l = lists.length; i < l; i++) {
          key = lists[i].type;
          cat = types[key] || (types[key] = {size: 0, kind: "type", name: key});
          cat.size++;
          for (j = 0, k = lists[i].tags.length; j < k; j++) {
            key = lists[i].tags[j];
            cat = tags[key] || (tags[key] = {size: 0, kind: "tag", name: key});
            cat.size++;
          }
        }
        scope.categories = values(tags).concat(values(types));
      });
    }

    function setMineDetails (mine) {
      timeout(function () { scope.serviceName = mine.ident; });
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

});
