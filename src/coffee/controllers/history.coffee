define ['lodash', './choose-dialogue'], (L, ChooseDialogueCtrl) ->
  injectables = ['$scope', '$http', '$location', '$routeParams', '$timeout', '$modal', 'meetRequest', 'Histories']

  Array injectables..., (scope, http, location, params, to, $modal, meetRequest, Histories) ->

    scope.nextTools = []
    scope.providers = []
    scope.collapsed = true # Hide details in reduced real-estate view.
    scope.items = {}
    scope.messages = {}
    scope.state = {}

    toolNotFound = (e) -> to -> scope.error = e

    currentCardinal = parseInt params.idx, 10
    scope.history = Histories.get id: params.id
    scope.steps = Histories.getSteps id: params.id
    scope.step = Histories.getStep id: params.id, idx: currentCardinal - 1
    scope.step.$promise.then ({tool}) ->
      http.get('/tools/' + tool)
          .then (({data}) -> scope.tool = data), toolNotFound
      http.get('/tools', {params: {capabilities: 'next'}})
          .then ({data}) -> scope.nextTools = data.filter (nt) -> nt.ident isnt tool
    http.get('/tools', {params: {capabilities: 'provider'}}).then ({data}) -> scope.providers = data

    scope.nextSteps = []

    scope.watchDeeply = (name, f) -> scope.$watch ((s) -> JSON.stringify s[name]), -> f s[name]

    scope.$watchCollection 'items', ->
      exporters = []
      for tool in scope.nextTools when tool.handles is 'items'
        for key, data of scope.items when data.ids.length
          exporters.push {key, data, tool}
      otherSteps = (s for s in scope.nextSteps when s.tool.handles isnt 'items')
      scope.nextSteps = otherSteps.concat(exporters)

    scope.$watchCollection 'messages', (msgs) ->
      handlers = L.values msgs
      otherSteps = (s for s in scope.nextSteps when s.kind isnt 'msg')
      scope.nextSteps = otherSteps.concat(handlers)

    scope.$watch 'list', ->
      listHandlers = []
      for tool in scope.nextTools when tool.handles is 'list'
        listHandlers.push {tool, data: scope.list}
      otherSteps = (s for s in scope.nextSteps when s.tool.handles isnt 'list')
      scope.nextSteps = otherSteps.concat(listHandlers)

    scope.saveHistory = ->
      scope.editing = false
      scope.history.$save()

    scope.updateHistory  = ->
      scope.editing = false
      scope.history = Histories.get id: params.id

    scope.setItems = (key, type, ids) -> to -> scope.items[key] = {type, ids}

    scope.hasSomething = (what, data, key) ->
      if what is 'list'
        to -> scope.list = data
      else
        for tool in scope.nextTools when tool.handles is what then do (tool, what, data, key) ->
          idx = tool.ident + what + key
          if scope.messages[idx]?
            scope.messages[idx].data = data
          else
            to -> scope.messages[idx] = {tool, data, kind: 'msg'}

    letUserChoose = (tools) ->
      dialogue = $modal.open
        templateUrl: '/partials/choose-tool-dialogue.html'
        controller: ChooseDialogueCtrl
        resolve: {items: tools}

      return dialogue.result

    scope.wantsSomething = (what, data) ->
      console.log "Something is wanted", what, data
      next = scope.providers.filter (t) -> t.handles is what
      console.log "Suitable providers found", next, scope.providers
      return unless next.length
      meetingRequest = if next.length is 1
        meetRequest(next[0], scope.step, data)
      else
        letUserChoose(next).then (provider) -> meetRequest provider, scope.step, data
      meetingRequest.then scope.nextStep

    scope.nextStep = (step) ->
      step = Histories.append {id: scope.history.id}, step, ->
        console.log "Created step " + step.id
        location.url "/history/#{ scope.history.id }/#{ currentCardinal + 1 }"

