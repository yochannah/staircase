define ['lodash'], (L) ->
  Array '$scope', '$http', '$routeParams', 'Histories', (scope, http, params, Histories) ->

    Histories.then (histories) ->
      scope.history = histories.get id: params.id
      scope.steps = histories.getSteps id: params.id
      scope.step = histories.getStep id: params.id, idx: params.idx - 1

      scope.saveHistory = ->
        scope.history.$save()
        scope.editing = false

