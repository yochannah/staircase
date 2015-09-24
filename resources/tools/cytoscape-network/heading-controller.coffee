define ['imjs'], ({Service}) -> Array '$scope', 'Mines', (scope, mines) ->

  scope.item = scope.data

  scope.showNetwork = ->
    step =
      title: "Chose interaction viewer for "
      tool: "cytoscape-network"
      data:
        service:
          root: scope.data.service.root
          token : scope.data.service.root
        id : scope.data.id
        item : scope.item

    scope.appendStep data: step
