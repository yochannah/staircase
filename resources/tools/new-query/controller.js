define(['angular', 'imjs'], function (ng, im) {

  var connect = im.Service.connect;

  return ['$scope', '$log', '$timeout', '$cacheFactory', 'Mines', 'ClassUtils',
          function (scope, logger, timeout, cacheFactory, mines, ClassUtils) {
    var countCache = (cacheFactory.get('query.counts') ||
                      cacheFactory('query.counts', {capacity: 100}));
    scope.classes = [];
    scope.serviceName = "";
    scope.startQuery = function () {
      logger.info("One " + scope.rootClass.className + " query please")
    };

    var fetchingDefaultMine = mines.get('default');

    scope.$watch('serviceName', function (name) {
      scope.tool.heading = 'Browse ' + name + ' by Data-Type';
    });

    scope.$watch(watchQuery, function (key) {
      var cachedN = countCache.get(key);
      if (cachedN != null) {
        setRowCount(cachedN);
      } else if (scope.connection) {
        setRowCount(null);
        scope.connection.count(getQuery(scope)).then(function (n) {
          setRowCount(countCache.put(key, n));
        });
      }
    });

    fetchingDefaultMine.then(setMineDetails);
    
    fetchingDefaultMine.then(connect).then(function (conn) {
      scope.connection = conn;
      conn.fetchModel().then(ClassUtils.setClasses(scope, groupOf.bind(null, scope), 'rootClass'));
    });

    function setRowCount (n) {
      timeout(function () { scope.rowCount = n });
    }

    function setMineDetails (mine) {
      timeout(function () { scope.serviceName = mine.ident; });
    }

  }];

  function groupOf (scope, cld) {
    var className = cld.className;
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
      query.select.push(scope.rootClass.className + ".*");
      if (scope.useConstraint && scope.fieldName && scope.fieldValue) {
        constraint.path = scope.rootClass.className + "." + scope.fieldName.name;
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
