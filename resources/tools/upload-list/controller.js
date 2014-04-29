define(['angular', 'imjs'], function (ng, im) {

  var connect = im.Service.connect;

  return ['$scope', '$log', '$timeout', '$cacheFactory', 'Mines', 'Histories', 'ClassUtils',
          function (scope, logger, timeout, cacheFactory, mines, histories, ClassUtils) {

    scope.classes = [];
    scope.navType = "pills";
    scope.rootClass = "";
    scope.rowCount = "counting...";
    scope.serviceName = "";

    var fetchingDefaultMine = mines.get('default');

    scope.$watch('serviceName', function (name) {
      scope.tool.heading = 'Resolve ' + scope.rootClass + ' identifiers at ' + name;
    });

    fetchingDefaultMine.then(setMineDetails);
    
    fetchingDefaultMine.then(connect).then(function (conn) {
      scope.connection = conn;
      conn.fetchModel()
          .then(ClassUtils.setClasses(scope, null, 'rootClass'))
          .then(classFilter(scope));
    });

    function setMineDetails (mine) {
      timeout(function () { scope.serviceName = mine.ident; });
    }

    function classFilter (scope) {
      return function (classes) {
        timeout(function () {
          var tables = scope.model.classes;
          scope.classes = classes.filter(function (c) {
            return !!tables[c.className].fields.id;
          });
        });
      };
    }

  }];

});
