define ['lodash'], (L) ->

  Array '$scope', (scope) ->

    scope.countFacets = (facetSet) -> L.keys(facetSet).length


