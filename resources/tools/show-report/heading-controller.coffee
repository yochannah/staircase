define [], -> Array '$scope', (scope) ->


  scope.$watch 'data', (data) ->
    console.log "show-report saw data", data
    scope.type = type = data.type
    scope.josh = "hello josh"



  scope.activate = ->
    scope.previousStep.$promise.then ->

      debugger;


      step =
        title: "Viewed report"
        tool: scope.tool.ident
        data: scope.data

      debugger;

      scope.appendStep data: step
