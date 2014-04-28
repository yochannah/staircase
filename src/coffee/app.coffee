deps = ['angular', './routes', 'angular-route', 'angular-ui', 'angular-ui-select2',
        './filters', './services', './directives', './controllers']

modules = [
  'ngRoute',
  'steps.controllers',
  'steps.services',
  'steps.filters',
  'steps.directives',
  'ui.bootstrap',
  'ui.select2']

$providers = [
  '$routeProvider', '$controllerProvider', '$compileProvider', '$filterProvider', '$provide'
]

define deps, (angular, router) ->
  Steps = angular.module('steps', modules)

  # Capture references to providers.
  Steps.config Array $providers..., (routes, controllers, directives, filters, provide) ->
    Steps.routes = routes
    Steps.controllers = controllers
    Steps.directives = directives
    Steps.filters = filters
    Steps.provide = provide

  router Steps

  return Steps

