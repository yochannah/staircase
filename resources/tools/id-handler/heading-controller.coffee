define ['imjs'], ({Service}) -> Array '$scope', '$timeout', 'Mines', 'makeList', (scope, to, Mines, makeList) ->

  scope.mineName = ""
  scope.typeName = scope.data?.request?.type

  scope.$watch 'data.service.root', (root) ->
    return unless root
    Mines.atURL(root).then (mine) -> to ->
      scope.mineName = mine.name
      scope.mine = Service.connect mine
      scope.mine.fetchModel().then (model) ->
        p = model.makePath(scope.data.request.type).getDisplayName().then (name) -> to ->
          scope.typeName = name

        p.then null, console.error.bind(console, 'Err')

  scope.showTable = ->
    step =
      title: "Ran #{ scope.typeName } query"
      tool: "show-table"
      data:
        service:
          root: scope.data.service.root
        query:
          from: scope.data.request.type
          select: ['*']
          constraints: [ {path: scope.data.request.type, op: 'IN', ids: scope.data.request.ids} ]

    scope.appendStep data: step

  scope.makeList = ->
    listReq =
      objectIds: scope.data.request.ids
      type: scope.data.request.type
      service: scope.data.service
    makeList.fromIds(listReq).then (list) ->
      step =
        title: "Created list #{ list.name }"
        tool: 'show-list'
        data:
          listName: list.name
          service: listReq.service

      scope.appendStep data: step

