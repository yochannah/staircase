deps = ['angular', './routes', 'angular-route', 'angular-ui', 'angular-animate', 'angular-ui-select2',
        './filters', './services', './directives', './controllers', 'angular-xeditable']

modules = [
  'ngSilent', # Must come before ngRoute
  'ngRoute',
  'ngAnimate',
  'steps.controllers',
  'steps.services',
  'steps.filters',
  'steps.directives',
  'ui.bootstrap',
  'ui.select2',
  'xeditable'
]

$providers = [
  '$routeProvider', '$controllerProvider', '$compileProvider',
  '$filterProvider', '$provide', '$sceDelegateProvider', 'stepConfigProvider'
]

define deps, (angular, router) ->
  Steps = angular.module('steps', modules)

  # Capture references to providers.
  Steps.config Array $providers..., (routes, controllers, directives, filters, provide, sceDelegateProvider, stepConfigProvider) ->
    Steps.routes = routes
    Steps.controllers = controllers
    Steps.directives = directives
    Steps.filters = filters
    Steps.provide = provide
    # Whitelist the sources of any tools we plan on using.
    sceDelegateProvider.resourceUrlWhitelist([
      'self', # TODO: make configurable.
      'http://*.labs.intermine.org/**',
      'http://alexkalderimis.github.io/**',
      'http://intermine.github.io/**'
    ])
    # Need to make this configurable.
    stepConfigProvider.configureStep 'show-list', activeTabs: ['enrich']
    stepConfigProvider.configureStep 'show-table',
      IndicateOffHostLinks: false,
      CellPreviewTrigger: 'click'
      ShowHistory: false # We manage history in the history list.
      Style:
        icons: 'fontawesome'

  router Steps

  Steps.run Array '$rootScope', '$http', (scope, http) ->
    scope.startingPoints = []
    editableOptions = {}
    editableOptions.theme = 'bs3'

    http.get("/tools", {params: {capabilities: "initial"}})
        .then ({data}) -> scope.startingPoints = data.map (tool) -> tool.active = true; tool

  return Steps

