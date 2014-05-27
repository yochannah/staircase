define(['angular', 'imjs', 'lodash'], function (ng, im, L) {

  var connect = im.Service.connect;

  return ['$scope', '$log', '$timeout', '$window', 'tokenise', 'Mines', 'ClassUtils',
          function (scope, logger, timeout, window, tokenise, mines, ClassUtils) {

    scope.applyExtension = function (region) {
      return region.chr + ':' +
                Math.max(0, region.start - scope.extension.value * scope.extension.unit.factor) +
                '..' +
                (region.end + scope.extension.value * scope.extension.unit.factor);
    };
    scope.extensionUnits = [
      {name: 'bases', factor: 1},
      {name: 'kb', factor: 1000},
      {name: 'Mb', factor: 1e6}
    ];
    scope.extension = {value: 0, unit: scope.extensionUnits[1]};
    scope.byLocation = ['chr', 'start', 'end'];
    scope.navType = "pills";
    scope.classes = [];
    scope.organisms = [];
    scope.serviceName = "";
    scope.regions = {pasted: null, file: null, parsed: null};

    scope.filesAreSupported = window.File && window.FileReader && window.FileList;

    scope.watchForTabs = function (event) {
      var key = event.keyCode
        , target = event.target;
      if (key === 9) {
        event.preventDefault();
        target.value = target.value + '\t';
      }
    };

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
            regions: scope.regions.parsed.map(scope.applyExtension),
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
        scope.regions.parsed = toRegions(
          L.uniq(tokenise(pasted, ['\n'])).filter(function(token) {
            return token.charAt(0) !== '#';
          })
        );
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
        scope.serviceName = mine.name;
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

  function toRegions (intervals) {
    return intervals.map(function parseInterval (interval) {
      var parts, match, region;
      if (match = (interval && interval.match(/^(\w+):(\d+)\.\.\.?(\d+)$/))) {
        return {
          interval: interval,
          chr: match[1],
          start: parseInt(match[2], 10),
          end: parseInt(match[3], 10)
        };
      } else if (match = (interval && interval.match(/^(\w+):(\d+)-(\d+)$/))) {
        return {
          interval: (match[1] + ':' + match[2] + '..' + match[3]),
          chr: match[1],
          start: parseInt(match[2], 10),
          end: parseInt(match[3], 10)
        };
      } else if (match = (interval && interval.match(/^(\w+):(\d+)$/))) {
        return {
          interval: interval + '..' + match[2],
          chr: match[1],
          start: parseInt(match[2], 10),
          end: parseInt(match[2], 10)
        };
      } else if (match = (interval && interval.match(/^(\w+)\t(\d+)\t(\d+)/))) {
        region = {
          chr: match[1],
          start: parseInt(match[2], 10),
          end: parseInt(match[3], 10)
        };
        region.interval = region.chr + ':' + region.start + '..' + region.end;
        return region;
      } else {
        parts = interval.split('\t');
        if (parts.length === 9) { // probably gff3
          return {
            interval: (parts[0] + ':' + parts[3] + '..' + parts[4]),
            chr: parts[0],
            start: parseInt(parts[3], 10),
            end: parseInt(parts[4], 10)
          };
        } else {
          return {
            interval: interval,
            invalid: true
          };
        }
      }
    });
  }
});


