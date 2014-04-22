define [], -> Array '$scope', '$http', (scope, http) ->

  scope.startingPoints = []

  http.get("/tools", {params: {capabilities: "initial"}})
      .then (tools) -> scope.startingPoints = tools

  scope.$apply()
