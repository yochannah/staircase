define(['angular', 'imjs'], function (ng, im) {

  var connect = im.Service.connect;

  return ['$scope', '$log', '$timeout', '$cacheFactory', 'Mines',
          function (scope, logger, timeout, cacheFactory, mines) {
    var countCache = cacheFactory('query.counts', {capacity: 100});
    scope.classes = [];
    scope.rowCount = "counting...";
    scope.serviceName = "";
    scope.startQuery = function () {logger.info("One " + scope.rootClass + " query please")};

    scope.groupOf = groupOf.bind(this, scope);

    var fetchingDefaultMine = mines.get('default');

    scope.$watch('serviceName', function (name) {
      scope.tool.heading = 'Browse ' + name + ' by Data-Type';
    });

    scope.$watch(watchQuery, function (key) {
      var cachedN = countCache.get(key);
      if (cachedN != null) {
        setRowCount(cachedN);
      } else if (scope.connection) {
        setRowCount("counting...");
        scope.connection.count(getQuery(scope)).then(function (n) {
          setRowCount(countCache.put(key, n));
        });
      }
    });

    fetchingDefaultMine.then(setMineDetails);
    
    fetchingDefaultMine.then(connect).then(function (conn) {
      scope.connection = conn;
      conn.fetchModel().then(setClasses);
    });

    function setRowCount (n) {
      timeout(function () { scope.rowCount = n });
    }

    function setMineDetails (mine) {
      timeout(function () { scope.serviceName = mine.ident; });
    }

    function setClasses (model) {
      timeout(function () {
        scope.model = model;
        scope.classes = Object.keys(model.classes).map(function (n) { return model.classes[n]; });
        if (model.name === "genomic") {
          scope.rootClass = model.classes.Gene;
        } else if (model.name === "testmodel") {
          scope.rootClass = model.classes.Employee;
        }
      });
    }
  }];

  function groupOf (scope, cld) {
    var className = cld.name;
    if (scope.model && "genomic" === scope.model.name) {
      if (className === "Gene" || className === "Protein") { // Hardcoded!! FIXME
        return "PRIMARY";
      } else {
        return "SECONDARY";
      }
    } else {
      return "";
    }
  }

  function getQuery (scope) {
    var constraint = {op: '='}, query = {select: []};
    if (scope.rootClass) {
      query.select.push(scope.rootClass.name + ".*");
      if (scope.useConstraint && scope.fieldName && scope.fieldValue) {
        constraint.path = scope.rootClass.name + "." + scope.fieldName.name;
        constraint.value = scope.fieldValue;
        query.where = [constraint];
      }
    }
    return query;
  }

  function watchQuery (scope) {
    return ng.toJson(getQuery(scope));
  }
});
