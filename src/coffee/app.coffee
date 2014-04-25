deps = ['angular', 'angular-route', 'angular-ui',
        'filters', 'services', 'directives', 'controllers']

modules = [
  'ngRoute',
  'steps.controllers',
  'steps.services',
  'steps.filters',
  'steps.directives',
  'ui.bootstrap']

$providers = [
  '$routeProvider', '$controllerProvider', '$compileProvider', '$filterProvider', '$provide'
]

define deps, (angular) ->
  Steps = angular.module('steps', modules)

  # Capture references to providers.
  Steps.config Array $providers..., (routes, controllers, directives, filters, provide) ->
    Steps.routes = routes
    Steps.controllers = controllers
    Steps.directives = directives
    Steps.filters = filters
    Steps.provide = provide

  return Steps

