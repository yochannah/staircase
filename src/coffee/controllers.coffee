define ['angular', 'lodash', 'angular-cookies', 'services'], (ng, L) ->
  
  Controllers = ng.module('steps.controllers', ['ngCookies', 'steps.services'])

  Controllers.controller 'FooterCtrl', Array '$scope', '$cookies', (scope, cookies) ->
    scope.showCookieMessage = cookies.ShowCookieWarning isnt "false"

    scope.$watch "showCookieMessage", -> cookies.ShowCookieWarning = String scope.showCookieMessage

  Controllers.controller 'WelcomeCtrl', Array '$scope', '$cookies', (scope, cookies) ->
    scope.showWelcome = cookies.ShowWelcome isnt "false"

    scope.$watch "showWelcome", (val) -> cookies.ShowWelcome = String val

  Controllers.controller 'IndexCtrl', ->

  Controllers.controller 'StartingPointCtrl', Array '$scope', '$routeParams', 'startHistory', (scope, params, startHistory) ->

    startHistory scope

    scope.$watch 'startingPoints', (startingPoints) -> if startingPoints
      scope.tool = L.find startingPoints, (sp) -> sp.ident is params.tool

  Controllers.controller 'AboutCtrl', Array '$http', '$scope', 'startHistory', (http, scope, startHistory) ->
    http.get('/tools', {params: {capabilities: 'initial'}})
        .then ({data}) ->
          scope.tool = L.chain(data).where(width: 1).find('description').value()
          scope.tool.expandable = false

    startHistory scope

  # Inline controller.
  Controllers.controller 'AuthController', Array '$rootScope', '$scope', 'Persona', (rs, scope, Persona) ->
    rs.auth ?= identity: null # TODO: put this somewhere more sensible.

    changeIdentity = (identity) ->
      scope.auth.identity = identity
      scope.auth.loggedIn = identity?

    scope.persona = new Persona {changeIdentity, loggedInUser: scope.auth.identity}

  Controllers.controller 'QuickSearchController', Array '$log', '$scope', (log, scope) ->
    scope.startQuickSearchHistory = (term) ->
      log.info "TERM", term # TODO: send this to interested parties.

  # Required controller.
  Controllers.controller('StartingPointsController', ['$scope', '$injector', ($scope, $injector) ->
    require ['controllers/starting-points'], (ctrl) -> $injector.invoke(ctrl, this, {$scope})
  ])

  Controllers.controller 'HistoryCtrl', Array '$scope', '$injector', ($scope, injector) ->
    require ['controllers/history'], (ctrl) -> injector.invoke(ctrl, this, {$scope})

  return Controllers

