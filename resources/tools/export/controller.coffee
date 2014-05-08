define [], -> Array '$scope', (scope) ->
  
  scope.type = scope.data.type

  scope.previousStep.$promise.then (step) ->
    console.log step.data

  if scope.data.ids
    scope.n = scope.data.ids.length

  scope.activate = ->
    console.log "Launch war rocket ajax to bring back his body!"

