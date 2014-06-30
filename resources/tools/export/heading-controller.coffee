define [], -> Array '$scope', (scope) ->
  
  scope.type = type = scope.data.type

  if scope.data.ids?
    scope.n = scope.data.ids.length
    scope.query =
      select: ['*']
      from: type
      where: [{path: type, op: 'IN', ids: scope.data.ids}]

  scope.activate = ->
    scope.previousStep.$promise.then ->

      step =
        title: "Exported results"
        tool: scope.tool.ident
        data:
          service:
            root: scope.previousStep.data.service.root
          query: scope.query
      scope.appendStep data: step

