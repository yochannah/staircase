(function() {
  define(function(require) {
    var Controllers, FacetCtrl, L, ng;
    ng = require('angular');
    L = require('lodash');
    Controllers = ng.module('steps.facets.controllers', []);
    return Controllers.controller('FacetCtrl', FacetCtrl = (function() {
      FacetCtrl.$inject = ['$scope'];

      function FacetCtrl(scope) {
        scope.countFacets = function(facetSet) {
          return L.keys(facetSet).length;
        };
      }

      return FacetCtrl;

    })());
  });

}).call(this);
