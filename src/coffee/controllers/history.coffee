define ['lodash'], (L) ->
  Array '$scope', '$http', '$location', '$routeParams', '$timeout', 'Histories', (scope, http, location, params, to, Histories) ->

    currentCardinal = parseInt params.idx, 10
    scope.history = Histories.get id: params.id
    scope.steps = Histories.getSteps id: params.id
    scope.step = Histories.getStep id: params.id, idx: currentCardinal - 1
    scope.step.$promise.then ({tool}) -> http.get(tool).then ({data}) ->
      scope.tool = data

    scope.nextTools = []
    scope.items = {}

    http.get('/tools', {params: {capabilities: 'next'}})
        .then ({data}) -> scope.nextTools = data

    scope.nextSteps = []

    scope.$watchCollection 'items', ->
      console.log "Items changed"
      exporters = []
      for tool in scope.nextTools when tool.handles is 'items'
        for key, data of scope.items when data.ids.length
          exporters.push {key, data, tool}
      otherSteps = (s for s in scope.nextSteps when s.tool.handles isnt 'items')
      scope.nextSteps = otherSteps.concat(exporters)

    scope.$watch 'list', ->
      listHandlers = []
      for tool in scope.nextTools when tool.handles is 'list'
        listHandlers.push {tool, data: scope.list}
      otherSteps = (s for s in scope.nextSteps when s.tool.handles isnt 'list')
      scope.nextSteps = otherSteps.concat(listHandlers)
      console.log listHandlers

    scope.saveHistory = ->
      scope.editing = false
      scope.history.$save()

    scope.updateHistory  = ->
      scope.editing = false
      scope.history = Histories.get id: params.id

    scope.setItems = (key, type, ids) -> to -> scope.items[key] = {type, ids}

    scope.hasList = (data) -> to -> scope.list = data

    scope.nextStep = (step) ->
      console.log "Appending step"
      step = Histories.append {id: scope.history.id}, step, ->
        console.log "Created step " + step.id
        location.url "/history/#{ scope.history.id }/#{ currentCardinal + 1 }"

