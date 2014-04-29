define(['angular', 'imjs'], function (ng, im) {

  var connect = im.Service.connect;

  return ['$scope', '$log', '$timeout', 'Mines', 'ClassUtils',
          function (scope, logger, timeout, mines, ClassUtils) {
    scope.navType = "pills";
    scope.classes = [];
    scope.organisms = [];
    scope.serviceName = "";

    scope.$on('act', function () {
      logger.info(scope.featureTypes, "in", scope.regions);
    });

    var fetchingDefaultMine = mines.get('default');

    scope.$watch('serviceName', function (name) {
      scope.tool.heading = 'Search ' + name + ' by chromosome location';
    });
    
    fetchingDefaultMine.then(setMineDetails);
    
    var connecting = fetchingDefaultMine.then(connect);
    
    connecting.then(function (conn) {
      conn.fetchModel()
          .then(ClassUtils.setClasses(scope, groupOf.bind(null, scope)))
          .then(filterToSequenceFeatures, logger.warn);
    });

    connecting.then(function (conn) {
      return conn.records({select: ['shortName', 'taxonId'], from: 'Organism'});
    }).then(function (orgs) {
      timeout(function () { scope.organisms = orgs; scope.organism = orgs[0];});
    }, logger.warn);

    function setMineDetails (mine) {
      timeout(function () { scope.serviceName = mine.ident; });
    }

    function filterToSequenceFeatures (classes) {
      timeout(function () {
        var i, l
          , model = scope.model
          , seqFeats = model.getSubclassesOf("SequenceFeature")
          , index = {};

        // Build index for fast lookups.
        for (i = 0, l = seqFeats.length; i < l; i++) {
          index[seqFeats[i]] = 1;
        }

        scope.classes = classes.filter(function isSeqFeature (names) {
          return !!index[names.className];
        });
        scope.featureTypes = ['Gene'];
      });
    }
  }];

  function groupOf (scope, cld) {
    var className = cld.className;
    return scope.model.classes[className].parents().join(', ');
  }
});


