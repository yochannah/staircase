define [], ->

  Array '$q', '$log', (Q, console) -> handleRequest = (previousStep, data) ->

    {request, service} = data

    step =
      title: "Ran query"
      tool: "show-table"
      data:
        query: request.query
        service: service

    console.debug step, data

    return Q.when(step)

