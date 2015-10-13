(function() {
  define(function(require) {
    var Controllers, QuickSearchController, ng;
    ng = require('angular');
    require('services');
    Controllers = ng.module('steps.quick-search.controllers', ['steps.services']);
    return Controllers.controller('QuickSearchController', QuickSearchController = (function() {
      QuickSearchController.$inject = ['$log', '$scope', 'historyListener'];

      function QuickSearchController(log, scope, _arg) {
        var startHistory;
        startHistory = _arg.startHistory;
        scope.startQuickSearchHistory = function(term) {
          return startHistory({
            thing: "for " + term,
            verb: {
              ed: "searched",
              ing: "searching"
            },
            tool: "keyword-search",
            data: {
              searchTerm: term
            }
          });
        };
      }

      return QuickSearchController;

    })());
  });

}).call(this);
