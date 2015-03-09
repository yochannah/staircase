define [], ->

  injectables = ['$q', '$log', 'makeList', 'identifyItem']

  factory = (Q, console, makeList, identifyItem) ->

    return handleRequest = (previousStep, data) ->
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
      else if data.item?
        # data :: {service, item}
        identifyItem(data.service, data.item).then (fields) ->
          d.resolve
            title: "Viewed item",
            tool: 'show-list'
            data:
              item:
                type: data.item.type
                fields: fields
              service: data.service
      else if data.request?.query?
        makeList.fromQuery(data.request.query, data.service).then (list) ->
          d.resolve
            title: "Created list #{ list.name }"
            tool: 'show-list'
            data:
              listName: list.name
              service: data.service
      else
        console.error "Don't know how to make a list from", data
        d.reject 'Unable to handle request'
      return d.promise

  return [injectables..., factory]
