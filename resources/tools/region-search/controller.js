define(['angular', 'imjs', 'lodash'], function (ng, im, L) {

  var connect = im.Service.connect;

  return ['$scope', '$log', '$timeout', '$window', 'tokenise', 'Mines', 'ClassUtils',
          function (scope, logger, timeout, window, tokenise, mines, ClassUtils) {
    scope.navType = "pills";
    scope.classes = [];
    scope.organisms = [];
    scope.serviceName = "";
    scope.regions = {pasted: null, file: null, parsed: null};

    scope.filesAreSupported = window.File && window.FileReader && window.FileList;

    scope.$on('act', function () {
      logger.info(scope.organism.shortName, scope.featureTypes, "in", scope.regions.parsed);
      scope.$emit('start-history', {
        thing: scope.regions.parsed.length + ' genomic intervals',
        verb: {
          ed: 'submitted',
          ing: 'submitting'
        },
        tool: 'region-search',
        data: {
          service: { root: scope.serviceRoot },
          request: {
            regions: scope.regions.parsed,
            types: scope.featureTypes,
            organism: scope.organism.shortName
          }
        }
      });
    });

    var fetchingDefaultMine = mines.get('default');

    scope.$watch('serviceName', function (name) {
      scope.tool.heading = 'Search ' + name + ' by chromosome location';
    });

    scope.$watch('regions.pasted', function (pasted) {
      timeout(function () {
        scope.regions.parsed = L.uniq(tokenise(pasted));
      });
    });

    scope.$watch('regions.file', function (file) {
      if (!file) return;
      var reader = new FileReader();
      reader.onloadend = function (e) {
        timeout(function () { // Set timeout to trigger digest.
          scope.regions.pasted = e.target.result;
        });
      };
      reader.readAsText(file);
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
      timeout(function () {
        scope.serviceRoot = mine.root;
        scope.serviceName = mine.ident;
      });
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


