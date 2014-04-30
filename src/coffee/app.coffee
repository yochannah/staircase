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
  '$routeProvider', '$controllerProvider', '$compileProvider',
  '$filterProvider', '$provide', '$sceDelegateProvider'
]

define deps, (angular, router) ->
  Steps = angular.module('steps', modules)

  # Capture references to providers.
  Steps.config Array $providers..., (routes, controllers, directives, filters, provide, sceDelegateProvider) ->
    Steps.routes = routes
    Steps.controllers = controllers
    Steps.directives = directives
    Steps.filters = filters
    Steps.provide = provide
    sceDelegateProvider.resourceUrlWhitelist([
      'self', # TODO: make configurable.
      'http://*.labs.intermine.org/**'
    ])

  router Steps

  return Steps

