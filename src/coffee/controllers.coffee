define ['angular', 'services'], (ng, services) ->
  
  Controllers = ng.module('steps.controllers', ['steps.services'])

  # Inline controller.
  Controllers.controller 'AuthController', Array '$rootScope', '$scope', 'Persona', (rs, scope, Persona) ->
    rs.auth ?= identity: null
    scope.persona = Persona
    onLogin = onLogout = -> console.log arguments

    Persona.watch {onlogin: onLogin, onlogout: onLogout, loggedInUser: scope.auth.identity}

  Controllers.controller 'QuickSearchController', Array '$log', '$scope', (log, scope) ->
    scope.startQuickSearchHistory = (term) ->
      log.info "TERM", term # TODO: send this to interested parties.

  # Required controller.
  Controllers.controller('StartingPointsController', ['$scope', '$injector', ($scope, $injector) ->
    require ['controllers/starting-points'], (ctrl) -> $injector.invoke(ctrl, this, {$scope})
  ])

  return Controllers

