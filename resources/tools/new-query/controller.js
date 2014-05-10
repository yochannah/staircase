define(['angular', 'lodash', 'imjs'], function (ng, L, im) {

  var connect = im.Service.connect;

  return ['$scope', '$log', '$timeout', '$cacheFactory', '$q', 'Mines', 'ClassUtils',
          function (scope, logger, timeout, cacheFactory, Q, mines, ClassUtils) {
    var countCache = (cacheFactory.get('query.counts') ||
                      cacheFactory('query.counts', {capacity: 100}));
    scope.defaults = {};
    scope.classes = [];
    scope.fields = [];
    scope.serviceName = "";

    scope.$on('act', function (evt) {
      scope.$emit('start-history', {
        thing: scope.rootClass.displayName + " query",
        verb: {
          ed: "ran",
          ing: "running"
        },
        tool: "show-table",
        data: {
          service: {
            root: scope.connection.root,
          },
          query: getQuery(scope)
        }
      });
    });

    scope.$on('reset', function (evt) {
      scope.rootClass = scope.defaults.rootClass;
      scope.useConstraint = false;
    });

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

    scope.$watch('rowCount', function (rowCount) {
      scope.state.disabled = false;
      if (rowCount === 0) {
        scope.state.disabled = true;
        scope.actions.act = "No results";
      } else if (rowCount === 1) {
        scope.actions.act = "View " + scope.rootClass.displayName;
      } else {
        scope.actions.act = "View Table";
      }
    });

    scope.$watch('rootClass.className', function (className) {
      var fields, promises;
      if (!scope.summaryFields) return;
      fields = scope.summaryFields[className];
      promises = fields.map(getDisplayName(scope.model));
      Q.all(promises).then(function (names) {
        scope.fields = L.zip(fields, names);
        scope.fieldName = scope.fields[0];
      });
    });

    fetchingDefaultMine.then(setMineDetails);
    
    fetchingDefaultMine.then(connect).then(function (conn) {
      scope.connection = conn;
      conn.fetchModel().then(ClassUtils.setClasses(scope, groupOf.bind(null, scope), 'rootClass'));
      conn.fetchSummaryFields().then(setSummaryFields);
    });

    function setRowCount (n) {
      timeout(function () { scope.rowCount = n });
    }

    function setMineDetails (mine) {
      timeout(function () { scope.serviceName = mine.ident; });
    }

    function setSummaryFields (summaryFields) {
      timeout(function () { scope.summaryFields = summaryFields });
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
        constraint.path = scope.fieldName[0];
        constraint.value = scope.fieldValue;
        query.constraints = [constraint];
      }
    }
    return query;
  }

  function watchQuery (scope) {
    return ng.toJson(getQuery(scope));
  }

  function getDisplayName (model) {
    return function (path) {
      return model.makePath(path).getDisplayName();
    };
  }
});
