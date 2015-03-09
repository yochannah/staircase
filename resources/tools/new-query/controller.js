define(['angular', 'lodash', 'imjs'], function (ng, L, im) {

  'use strict';

  var connect = im.Service.connect;

  return [
    '$scope', '$log', '$timeout', '$q',
    'Mines', 'ClassUtils', 'Messages', 'localCache',
    Ctrl
  ];

  function Ctrl (scope, logger, timeout, Q, mines, ClassUtils, messages, localCache) {

    var countCache = localCache('query.counts');

    scope.defaults = {};
    scope.classes = [];
    scope.fields = [];
    scope.serviceName = "";
    scope.state || (scope.state = {});
    scope.actions || (scope.actions = {});

    scope.showResults = function (evt) {
      scope.$emit('start-history', {
        thing: scope.state.rootClass.displayName + " query",
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
    };
    scope.$on('act', scope.showResults);

    scope.$on('reset', function (evt) {
      scope.state.rootClass = scope.defaults.rootClass;
      scope.useConstraint = false;
    });

    var mineName = 'default';
    if (scope.tool.args && scope.tool.args.service) {
      mineName = scope.tool.args.service;
    }
    var fetchingDefaultMine = mines.get(mineName);

    scope.$watch('serviceName', function (name) {
      scope.tool.heading = 'Browse ' + name + ' by Data-Type';
    });

    fetchingDefaultMine.then(setMineDetails);
    
    var mineInitialised = fetchingDefaultMine.then(connect).then(function (conn) {
      scope.connection = conn;
      conn.fetchModel()
          .then(ClassUtils.setClasses(scope, groupOf.bind(null, scope), 'rootClass'))
          .then(function () {
            scope.state.rootClass = scope.rootClass;
            L.defer(scope.$apply.bind(scope));
          });
      conn.fetchSummaryFields().then(setSummaryFields);
      return conn.fetchRelease().then(function (release) {
        scope.releaseString = release;
      });
    });

    scope.$watch(watchQuery, function (queryString) {
      mineInitialised.then(function () {
        // Counts are stable within releases. We know that our queries don't
        // have any list constraints in them, which is the exception to this
        // rule.
        var key = mineName + ':' + scope.releaseString + ':' + queryString;
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
    });

    scope.$watch('rowCount', function (rowCount) {
      scope.state.disabled = false;
      if (rowCount === 0) {
        scope.state.disabled = true;
        scope.actions.act = "No results";
      } else if (rowCount === 1) {
        scope.actions.act = "View " + scope.state.rootClass.displayName;
      } else {
        scope.actions.act = "View Table";
      }
    });

    scope.$watch('state.rootClass.className', function (className) {
      var fields, promises;
      if (!scope.summaryFields) return;
      fields = [className].concat(scope.summaryFields[className]);
      promises = fields.map(getDisplayName(scope.model));
      Q.all(promises).then(function (names) {
        scope.fields = L.zip(fields, names);
        scope.fieldName = scope.fields[0];
      });
    });


    function setRowCount (n) {
      scope.rowCount = n;
      L.defer(scope.$apply.bind(scope));
    }

    function setMineDetails (mine) {
      scope.serviceName = mine.name;
      L.defer(scope.$apply.bind(scope));
    }

    function setSummaryFields (summaryFields) {
      scope.summaryFields = summaryFields;
      L.defer(scope.$apply.bind(scope));
    }

  }

  // This should read class tags.
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
    var className, constraint = {op: '='}, query = {select: []};
    if (scope.state.rootClass) {
      className = scope.state.rootClass.className;
      query.select.push(className + ".*");
      if (scope.useConstraint && scope.fieldName && scope.fieldValue) {
        constraint.path = scope.fieldName[0];
        constraint.value = scope.fieldValue;
        if (scope.fieldName[0] === className) constraint.op = 'LOOKUP';
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
