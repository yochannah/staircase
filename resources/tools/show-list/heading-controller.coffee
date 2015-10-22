define ['imjs'], ({Service}) -> Array '$scope', 'Mines', 'makeList', (scope, mines, makeList) ->

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


  scope.queryList = ->
    makeList.fromQuery(scope.list.query, scope.list.service).then (list) ->
      step =
        title: "Created list #{ list.name }"
        tool: 'show-list'
        data:
          listName: list.name
          service: scope.list.service
          
      scope.appendStep data: step
