define (require, exports, module) ->

  angular = require 'angular'
  router = require './routes'
  L = require 'lodash'
  require 'angular-route'
  require 'angular-resource'
  require 'angular-ui'
  require 'angular-animate'
  require 'angular-ui-select2'
  require 'ng-headroom'
  require 'angular-silent'
  require 'angular-notify'
  require 'angular-cookies'
  require 'angular-local-storage'
  require './filters'
  require './services'
  require './directives'
  require './controllers'
  ga = require 'analytics'

  # Whitelist the sources of any tools we plan on using.
  whiteList = [
    'self', # TODO: make configurable.
    'http://*.labs.intermine.org/**',
    'http://tools.intermine.org/**',
    'http://alexkalderimis.github.io/**',
    'http://intermine.github.io/**'
  ]

  # TODO: this needs to come from config
  stepConfig =
    'show-list':
      activeTabs: ['enrich']
    'show-table':
      TableCell:
        IndicateOffHostLinks: false,
        PreviewTrigger: 'click'
      ShowHistory: false # We manage history in the history list.
      Style:
        icons: 'fontawesome'

  APP_NAME = 'steps' # TODO: needs to be configurable.

  module.exports = Steps = angular.module 'steps', [
    'ngSilent', # Must come before ngRoute
    'ngRoute',
    'ngAnimate',
    'steps.controllers',
    'steps.services',
    'steps.filters',
    'steps.directives',
    'ui.bootstrap',
    'ui.select2',
    'LocalStorageModule',
    'headroom',
    'cgNotify'
  ]

  Steps.config Array '$routeProvider', (routes) -> Steps.routes = routes
  Steps.config Array '$controllerProvider', (cs) -> Steps.controllers = cs
  Steps.config Array '$compileProvider', (p) -> Steps.directives = p
  Steps.config Array '$filterProvider', (p) -> Steps.filters = p
  Steps.config Array '$provide', (p) -> Steps.provide = p
  Steps.config Array '$sceDelegateProvider', (p) ->
    p.resourceUrlWhitelist whiteList
  Steps.config Array 'stepConfigProvider', (p) ->
    for step, conf of stepConfig
      p.configureStep step, conf
  Steps.config Array 'localStorageServiceProvider', (p) -> p.setPrefix APP_NAME

  # Define routes
  router Steps

  Steps.run Array '$rootScope', '$http', '$window', '$location', (scope, http, $window, $loc) ->
    scope.startingPoints = []

    scope.$on '$routeChangeSuccess', (event, route) ->
      $window.scrollTo 0, 0
      ga('send', 'pageview', $loc.path())

    http.get("/tools", {params: {capabilities: "initial"}}).then ({data}) ->
      scope.startingPoints = data.map (tool) -> tool.active = true; tool

    ga 'send', 'event', 'init', $loc.path()

