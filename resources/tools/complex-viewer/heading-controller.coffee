define [], -> Array '$scope', (scope) ->

  scope.$watch 'data', (data) ->
    console.log "complex-viewer saw data", data
    scope.type = type = data.type

  scope.activate = ->
    scope.previousStep.$promise.then ->
      scope
      step =
        title: "Protein Features"
        description: "Viewed complex for " + scope.data.type
        tool: scope.tool.ident
        data: scope.data

      scope.appendStep data: step

      #todo: add gene/protein name to the description
