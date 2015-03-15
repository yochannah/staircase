define [], ->

  Array '$q', '$log', (Q, console) -> handleRequest = (previousStep, data) ->

    step =
      title: (data.title ? "Ran query")
      tool: "show-table"
      data:
        query: (data.query or data.request.query)
        service: data.service

    console.debug step, data

    return Q.when(step)

