define ['angular', 'angular-cookies', 'services'], (ng) ->
  
  Controllers = ng.module('steps.controllers', ['ngCookies', 'steps.services'])

  Controllers.controller 'FooterCtrl', Array '$scope', '$cookies', (scope, cookies) ->
    scope.showCookieMessage = cookies.ShowCookieWarning isnt "false"

    scope.$watch "showCookieMessage", -> cookies.ShowCookieWarning = String scope.showCookieMessage

  Controllers.controller 'WelcomeCtrl', Array '$scope', '$cookies', (scope, cookies) ->
    scope.showWelcome = cookies.ShowWelcome isnt "false"

    scope.$watch "showWelcome", (val) -> cookies.ShowWelcome = String val


  Controllers.controller 'IndexCtrl', ->

  Controllers.controller 'AboutCtrl', ->

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

