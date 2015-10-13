(function() {
  define(function(require) {
    var L, StartingPointCtrl, StartingPoints, ng;
    ng = require('angular');
    L = require('lodash');
    require('services');
    StartingPoints = ng.module('steps.starting-point.controller', ['steps.services']);
    StartingPointCtrl = (function() {
      StartingPointCtrl.$inject = ['$scope', '$routeParams', 'historyListener'];

      function StartingPointCtrl(scope, params, historyListener) {
        var byIdent, filter;
        historyListener.watch(scope);
        byIdent = function(_arg) {
          var ident;
          ident = _arg.ident;
          return ident === params.tool;
        };
        if (params.service != null) {
          filter = function(sp) {
            var _ref;
            return byIdent(sp) && params.service === ((_ref = sp.args) != null ? _ref.service : void 0);
          };
        } else {
          filter = byIdent;
        }
        scope.$watch('startingPoints', function(startingPoints) {
          var tool;
          if (startingPoints) {
            tool = L.find(startingPoints, filter);
            if (tool) {
              scope.tool = Object.create(tool);
              return scope.tool.state = 'FULL';
            }
          }
        });
      }

      return StartingPointCtrl;

    })();
    return StartingPoints.controller('StartingPointCtrl', StartingPointCtrl);
  });

}).call(this);
