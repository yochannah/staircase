define ['imjs'], ({Service}) -> Array '$scope', 'Mines', (scope, mines) ->
  
  scope.type = scope.data.type
  scope.services = []

  scope.previousStep.$promise.then (step) ->
    # TODO: make this consistent.
    origin = (step.data.url or step.data.root or step.data.service.root)
    mines.all().then (services) ->
      scope.services = (s for s in services when Service.connect(s).root isnt origin)

  scope.activate = ->
    console.log "Launch war rocket ajax to bring back his body!"

