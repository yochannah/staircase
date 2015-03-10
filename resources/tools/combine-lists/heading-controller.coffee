define ['imjs'], ({Service}) -> Array '$scope', 'connectTo', (scope, connectTo) ->
  
  scope.type = scope.data.type
  scope.services = []

  step = scope.previousStep
  # TODO: make this consistent.
  origin = (step.data.url or step.data.root or step.data.service.root)
  connectTo(origin).then (s) -> scope.service = s

  scope.activate = ->
    console.log "Launch war rocket ajax to bring back his body!"

