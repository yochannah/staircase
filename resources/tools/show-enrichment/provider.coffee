define [], ->

  handleRequest = Array '$q', (Q) -> (previousStep, data) ->
    Q.when
      title: "Ran enrichment query #{ data.request.enrichment }"
      tool: "show-enrichment"
      data: data

  return handleRequest

