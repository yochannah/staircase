define [], -> Array '$scope', '$http', (scope, http) ->

  scope.tools = []
  http.get("/tools", {params: {capabilities: "initial"}})
      .then (tools) -> scope.tools = tools

  scope.$apply()
