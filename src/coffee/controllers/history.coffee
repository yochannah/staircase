define ['lodash'], (L) ->
  Array '$scope', '$http', '$routeParams', 'Histories', (scope, http, params, Histories) ->

    scope.history = Histories.get id: params.id
    scope.steps = Histories.getSteps id: params.id
    scope.step = Histories.getStep id: params.id, idx: params.idx - 1
    scope.step.$promise.then ({tool}) -> http.get(tool).then ({data}) ->
      scope.tool = data

    scope.saveHistory = ->
      scope.editing = false
      scope.history.$save()

    scope.updateHistory  = ->
      scope.editing = false
      scope.history = Histories.get id: params.id

