define ['angular', 'services'], (ng, services) ->

  Filters = ng.module('steps.filters', ['steps.services'])

  Filters.filter('interpolate', ['version', (v) -> (t) -> String(t).replace(/\%VERSION\%/mg, v)])

  return Filters
