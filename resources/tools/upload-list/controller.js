define(['angular', 'imjs', 'lodash'], function (ng, im, L) {
  'use strict';

  var connect = im.Service.connect;

  return [
    '$scope', '$log', '$timeout', '$cacheFactory', '$window',
    '$filter', 'tokenise', 'Mines', 'Histories', 'ClassUtils',
    UploadListCtrl
  ];

  function UploadListCtrl (
    scope, logger, timeout, cacheFactory, window,
    filters, tokenise, mines, histories, ClassUtils
    ) {

    scope.classes = [];
    scope.extraValues = [];
    scope.discriminator = null;
    scope.extraValue = null;
    scope.parsedIds = [];
    scope.navType = "pills";
    scope.sorting = '';
    scope.ids = {pasted: '', file: null};
    scope.rowCount = "counting...";
    scope.serviceName = "";
    scope.state || (scope.state = {rootClass: {}});
    scope.actions || (scope.actions = {});

    scope.filesAreSupported = window.File && window.FileReader && window.FileList;

    scope.sendIdentifiers = function (evt) {
      var identifiers = L.pluck(scope.parsedIds, 'token');
      var type = scope.state.rootClass.className;
      scope.$emit('start-history', {
        thing: identifiers.length + ' ' + scope.state.rootClass.displayName + ' identifiers',
        verb: {
          ed: 'submitted',
          ing: 'submitting'
        },
        tool: 'resolve-ids',
        data: {
          service: { root: scope.connection.root },
          request: {
            type: type,
            identifiers: identifiers,
            extra: (scope.extraValue || ''),
            caseSensitive: false
          }
        }
      });
    };

    scope.$on('act', scope.sendIdentifiers);

    var fetchingDefaultMine = mines.get('default');

    scope.removeToken = function (token) {
      scope.parsedIds = L.without(scope.parsedIds, token);
    };

    scope.addToken = function () {
      scope.parsedIds.push({token: '', editing: true});
    };

    scope.$watch('parsedIds', function (ids) {
      scope.state.disabled = !(ids && ids.length);
      if (!scope.state.disabled) {
        scope.actions.act = "Send " + filters('number')(ids.length) + " identifiers";
      } else {
        scope.actions.act = null;
      }
    });

    scope.$watch('serviceName', function (name) {
      scope.tool.heading = 'Resolve identifiers at ' + name;
    });

    scope.$watch('ids.pasted', function (ids) {
      scope.parsedIds = L.uniq(tokenise(ids)).map(function (token) {
        return {token: token};
      });
    });

    scope.$watch('state.rootClass.className', function (className) {
      if (!className) return;
      scope.extraValues = [];
      scope.discriminator = null;
      scope.extraValue = null;
      scope.connection.fetchSummaryFields().then(function withSummaryFields(fields) {
        var fieldsForType = fields[className];
        var joinedFields = fieldsForType.filter(function (f) { return f.split('.').length > 2; });
        console.log(fieldsForType, joinedFields);
        if (joinedFields.length === 1) {
          scope.connection.fetchModel().then(function (model) {
            return model.makePath(joinedFields[0]).getDisplayName();
          }).then(function (name) {
            timeout(function () {
              scope.discriminator = name;
            });
          });
          scope.connection.values({select: joinedFields}).then(function (values) {
            timeout(function () {
              scope.extraValues = values;
            });
          });
        }
      });
    });

    scope.$watch('ids.file', function (file) {
      if (!file) return;
      var reader = new FileReader();
      reader.onloadend = function (e) {
        timeout(function () { // Set timeout to trigger digest.
          scope.ids.pasted = e.target.result;
        });
      };
      reader.readAsText(file);
    });

    fetchingDefaultMine.then(setMineDetails);
    
    fetchingDefaultMine.then(connect).then(function (conn) {
      scope.connection = conn;
      conn.fetchModel()
          .then(ClassUtils.setClasses(scope, null, 'state.rootClass'))
          .then(classFilter(scope));
    });

    function setMineDetails (mine) {
      timeout(function () { scope.serviceName = mine.name; });
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

  }

});
