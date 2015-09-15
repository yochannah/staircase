define ['imjs'], ({Service}) -> Array '$scope', 'Mines', (scope, mines) ->

  scope.item = scope.data

  scope.showNetwork = ->
    step =
      title: "Chose network interaction viewer"
      tool: "cytoscape-network"
      data:
        service:
          root: scope.data.service.root
          token : scope.data.service.root
        id : scope.data.id

    scope.appendStep data: step
