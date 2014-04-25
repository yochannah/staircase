define(['angular', 'imjs'], function (ng, im) {

  var connect = im.Service.connect;

  return ['$scope', '$log', '$timeout', '$cacheFactory', 'Mines',
          function (scope, logger, timeout, cacheFactory, mines) {

    scope.classes = [];
    scope.navType = "pills";
    scope.rootClass = "";
    scope.rowCount = "counting...";
    scope.serviceName = "";
    scope.uploadIds = function () {logger.info("One " + scope.rootClass + " id job please")};

    var fetchingDefaultMine = mines.get('default');

    scope.$watch('serviceName', function (name) {
      scope.tool.heading = 'Resolve ' + scope.rootClass + ' identifiers at ' + name;
    });

    fetchingDefaultMine.then(setMineDetails);
    
    fetchingDefaultMine.then(connect).then(function (conn) {
      scope.connection = conn;
      conn.fetchModel().then(setClasses);
    });

    function setMineDetails (mine) {
      timeout(function () { scope.serviceName = mine.ident; });
    }

    function setClasses (model) {
      timeout(function () {
        scope.model = model;
        scope.classes = Object.keys(model.classes).filter(classFilter.bind(null, model));
        if (model.name === "genomic") {
          scope.rootClass = "Gene";
        } else if (model.name === "testmodel") {
          scope.rootClass = "Employee";
        }
      });
    }

  }];

  function classFilter (model, name) {
    return !!model.classes[name].fields.id;
  }

});
