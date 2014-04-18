define ['angular', 'services'], (ng, services) ->
  
  Controllers = ng.module('steps.controllers', ['steps.services'])

  # Inline controller.
  Controllers.controller 'AuthController', Array '$scope', 'Persona', (scope) ->
    onLogin = -> # TODO
    onLogout = -> # TODO
    Persona.watch scope.auth.identity, onLogin, onLogout

  Controllers.controller 'QuickSearchController', Array '$log', '$scope', (log, scope) ->
    scope.startQuickSearchHistory = (term) ->
      log.info "TERM", term # TODO: send this to interested parties.

  # Required controller.
  Controllers.controller('StartingPointsController', ['$scope', '$injector', ($scope, $injector) ->
    require ['controllers/starting-points'], (ctrl) -> $injector.invoke(ctrl, this, {$scope})
  ])

  return Controllers

