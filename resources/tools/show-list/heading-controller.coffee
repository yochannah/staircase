define ['imjs'], ({Service}) -> Array '$scope', 'Mines', (scope, mines) ->

  console.log('SHOW LIST CONTROLLER', scope.data)

  scope.list = scope.data

  scope.activate = ->
    step =
      title: "Created list " + scope.list.name
      tool: "show-list"
      data:
        service:
          root: scope.list.root
        listName: scope.list.name

    scope.appendStep data: step
