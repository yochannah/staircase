define (require) ->
  
  ng = require 'angular'
  L = require 'lodash'

  Controllers = ng.module 'steps.facets.controllers', []

  Controllers.controller 'FacetCtrl', class FacetCtrl

    @$inject = ['$scope']

    constructor: (scope) ->
      scope.countFacets = (facetSet) -> L.keys(facetSet).length

