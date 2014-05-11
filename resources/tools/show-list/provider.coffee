define [], ->

  handleRequest = Array '$q', '$log', 'makeList', (Q, log, makeList) -> (previousStep, data) ->
    d = Q.defer()
    if data.objectIds?
      category = previousStep.data.request?.extra
      makeList.fromIds(data, category).then (list) ->
        d.resolve
          title: "Created list #{ list.name }"
          tool: 'show-list'
          data:
            listName: list.name
            service: data.service
    else
      log.error "Don't know how to make a list from", data
      d.reject 'Unable to handle request'
    return d.promise

  return handleRequest
