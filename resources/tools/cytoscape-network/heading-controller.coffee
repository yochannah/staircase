define ['imjs'], ({Service}) -> Array '$scope', 'Mines', (scope, mines) ->

  scope.list = scope.data

  scope.showNetwork = ->
    step =
      title: "Chose network interaction viewer"
      tool: "cytoscape-network"
      data:
        service:
          root: scope.list.root
        listName: scope.list.name

    scope.appendStep data: step
