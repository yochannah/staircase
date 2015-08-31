define [], -> Array '$scope', (scope) ->


  scope.$watch 'data', (data) ->
    console.log "show-report saw data", data
    scope.type = type = data.type


  scope.activate = ->
    scope.previousStep.$promise.then ->
      step =
        title: "Viewed report"
        tool: scope.tool.ident
        data: scope.data

      scope.appendStep data: step
