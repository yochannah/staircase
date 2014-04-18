define ['angular', 'services'], (ng, services) ->

  Directives = ng.module('steps.directives', ['steps.services'])

  Directives.directive 'appVersion', ['version', (v) -> (scope, elm) -> elm.text(version)]

  return Directives
