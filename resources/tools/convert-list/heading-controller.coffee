define ['imjs'], ({Service}) -> Array '$scope', 'notify', 'Mines', (scope, notify, mines) ->
  
  scope.type = scope.data.type
  scope.services = []

  step = scope.previousStep
  # TODO: make this consistent.
  origin = (step.data.url or step.data.root or step.data.service.root)
  mines.all().then (services) ->
    scope.services = (Object.create(s) for s in services \
                                      when Service.connect(s).root isnt origin)

  scope.activate = (service) ->
    service.running = true
    notify "Rummages through #{ service.name }"


