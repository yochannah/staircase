define(['angular', 'imjs', 'lodash'], function (ng, im, L) {
  'use strict';

  var connect = im.Service.connect;
  var nameTemplate = L.template('Upload list to <%= name %>');
  var actionTemplate = L.template('Send <%= format(numIds) %> <%= type %> identifiers');

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
    scope.sorting = '';
    scope.ids = {pasted: '', file: null};
    scope.rowCount = "counting...";
    scope.serviceName = "";
    scope.state || (scope.state = {rootClass: {}});
    scope.actions || (scope.actions = {});

    this.focusTextArea = function () {
      this.textAreaFocussed = true;
    };

    var mineName = 'default';
    console.log(scope.tool);
    if (scope.tool.args && scope.tool.args.service) {
      mineName = scope.tool.args.service;
    }
    var fetchingDefaultMine = mines.get(mineName);

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
            caseSensitive: scope.caseSensitive
          }
        }
      });
    };

    scope.$on('act', scope.sendIdentifiers);

    scope.removeToken = function (token) {
      scope.parsedIds = L.without(scope.parsedIds, token);
    };

    scope.addToken = function () {
      scope.parsedIds.push({token: '', editing: true});
    };

    scope.$watch('parsedIds', updateActionButton);
    scope.$watch('state.rootClass.displayName', updateActionButton);

    function updateActionButton () {
      var ids = scope.parsedIds;
      scope.state.disabled = !(ids && ids.length);
      if (!scope.state.disabled) {
        scope.actions.act = actionTemplate({
          format: filters('number'),
          numIds: ids.length,
          type: (scope.state.rootClass.displayName)
        });
      } else {
        scope.actions.act = null;
      }
    }

    scope.$watch('serviceName', function (name) {
      scope.tool.heading = nameTemplate({name: name});
    });

    scope.$watch('ids.pasted', function (ids) {
      scope.parsedIds = L.uniq(tokenise(ids)).map(function (token) {
        return {token: token};
      });
    });

    /** TODO! Find a way to automatically work out the extra value **/
    scope.$watch('state.rootClass.className', function (className) {
      if (!className) return;
      scope.extraValues = [];
      scope.discriminator = null;
      scope.extraValue = null;
      scope.connection.fetchModel().then(function (model) {
        if (!model.classes[className].fields.organism) {
          return;
        }
        var path = model.makePath('Organism.shortName');
        path.getDisplayName().then(function (name) {
          timeout(function () {
            scope.discriminator = name;
          });
        });
        scope.connection
             .values({select: ['Organism.shortName']})
             .then(function (values) { timeout(function () {
                scope.extraValues = values;
              });
            });
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
