require ['angular', 'services'], (ng) ->

  Directives = ng.module('steps.directives', ['steps.services'])

  Directives.directive 'appVersion', ['version', (v) -> (scope, elm) -> elm.text(version)]

  Directives.directive 'nativeTool', ->
    restrict: 'E'
    scope:
      templateURI: '=templateURI'
      controllerURI: '=controllerURI'

