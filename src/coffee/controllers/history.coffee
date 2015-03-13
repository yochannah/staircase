define ['lodash', 'imjs', 'analytics', './choose-dialogue'], (L, imjs, ga, ChooseDialogueCtrl) ->
  
  # Run-time requirements
  injectables = L.pairs
    scope: '$scope'
    route: '$route'
    http: '$http'
    location: '$location',
    params: '$routeParams'
    to: '$timeout'
    console: '$log'
    Q: '$q'
    $modal: '$modal'
    meetRequest: 'meetRequest'
    Histories: 'Histories'
    Mines: 'Mines'
    silentLocation: '$ngSilentLocation'
    serviceStamp: 'serviceStamp'
    notify: 'notify'

  #--- Functions.
 
  # Connect to a service and assign it a name.
  connectWithName = (conf) ->
    service = imjs.Service.connect conf
    service.name = conf.name
    return service

  # Test to see if two strings overlap.
  overlaps = (x, y) -> x && y && (x.indexOf(y) >= 0 || y.indexOf(x) >= 0)

  # Get the configured service at the given URL.
  atURL = (url) -> (mines) -> L.find mines, (m) -> overlaps m.root, url

  # Turn a configuration object into a tool.
  # Currently just adds a handles() method.
  toTool = (conf) ->
    handles = conf.handles
    providers = conf.provides

    if Array.isArray handles
      conf.handles = (cat) -> handles.indexOf(cat) >= 0
    else
      conf.handles = (cat) -> handles is cat

    if Array.isArray providers
      conf.provides = (x) -> x in providers
    else
      conf.provides = (x) -> x is providers
    
    conf

  #--- Controller, exported as return value
  
  class HistoryController

    currentCardinal: 0

    @$inject: (snd for [fst, snd] in injectables)

    constructor: (injected...) ->
      for [name, _], idx in injectables
        @[name] = injected[idx]

      @init()
      @startWatching()

    startWatching: ->
      scope = @scope

      scope.$watchCollection 'items', ->
        exporters = []
        for tool in scope.nextTools when tool.handles 'items'
          for key, data of scope.items when data.ids.length
            exporters.push {key, data, tool}
        otherSteps = (s for s in scope.nextSteps when not s.tool.handles 'items')
        scope.nextSteps = otherSteps.concat(exporters)

      scope.$watchCollection 'messages', (msgs) ->
        handlers = L.values msgs
        otherSteps = (s for s in scope.nextSteps when s.kind isnt 'msg')
        scope.nextSteps = otherSteps.concat(handlers)

      scope.$watch 'list', ->
        listHandlers = []
        for tool in scope.nextTools when tool.handles 'list'
          listHandlers.push {tool, data: scope.list}
        otherSteps = (s for s in scope.nextSteps when not s.tool.handles 'list')
        scope.nextSteps = otherSteps.concat(listHandlers)

    init: ->
      {Histories, Mines, params, http} = @

      # See below for data fetching to fill these.
      @scope.nextTools ?= []
      @scope.providers ?= []
      @scope.messages ?= {}
      @scope.nextSteps ?= []
      @scope.items ?= {}

      @scope.collapsed = true # Hide details in reduced real-estate view.
      @scope.state = {expanded: false, nextStepsCollapsed: true}
      @currentCardinal = parseInt params.idx, 10
      @scope.history = Histories.get id: params.id
      @scope.steps = Histories.getSteps id: params.id
      @scope.step = Histories.getStep id: params.id, idx: @currentCardinal - 1
      @mines = Mines.all()

      toolNotFound = (e) => @to => @scope.error = e

      unless @scope.tool? # tool never changes! to do so breaks the history API.
        @scope.step.$promise.then ({tool}) =>
          http.get('/tools/' + tool)
                .then (({data}) => @scope.tool = data), toolNotFound
          http.get('/tools', params: {capabilities: 'next'})
              .then ({data}) -> data.filter((t) -> t.ident isnt tool).map toTool
              .then (tools) => @scope.nextTools = tools
      http.get('/tools', params: {capabilities: 'provider'})
          .then ({data}) -> data.map toTool
          .then (providers) => @scope.providers = providers

    saveHistory: ->
      @scope.editing = false
      @scope.history.$save()

    updateHistory: ->
      {Histories, params} = @
      @scope.editing = false
      @scope.history = Histories.get id: params.id

    setItems: -> (key, type, ids) => @set ['items', key], {type, ids}

    # Set scope values using an array of keys.
    # eg: this.set ['foo', 'bar'], 2 == this.scope.foo.bar = 2
    #       (but in a timeout)
    set: ([prekeys..., key], value) -> @to =>
      o = @scope
      for k, i in prekeys
        o = o[k]
      o[key] = value

    hasSomething: (what, data, key) ->
      {scope, console, to, mines} = @
      console.debug "Anybody want some #{ what }?"
      if what is 'list'
        return to -> scope.list = data

      for tool in scope.nextTools when tool.handles(what) then do (tool) =>
        idx = tool.ident + what + key
        if scope.messages[idx]?
          console.debug "replacing message data: #{ data }"
          @set ['messages', idx, 'data'], data
        else
          @mines.then(atURL(data['service:base'] ? data.service.root))
                .then(connectWithName)
                .then (service) => @set ['messages', idx], {tool, data, service, kind: 'msg'}

    letUserChoose: (tools) ->
      dialogue = @$modal.open
        templateUrl: '/partials/choose-tool-dialogue.html'
        controller: ChooseDialogueCtrl
        resolve: {items: tools}

      return dialogue.result

    meetingRequest: (next, data) ->
      if next.length is 1
        @meetRequest(next[0], @scope.step, data)
      else
        @letUserChoose(next).then (provider) => @meetRequest provider, @scope.step, data

    wantsSomething: (what, data) ->
      {notify, console, meetRequest} = @
      console.log "Something is wanted", what, data
      next = @scope.providers.filter (t) -> t.provides what
      console.log "Suitable providers found", next, @scope.providers

      return unless next.length

      @meetingRequest(next, data).then @nextStep, (err) ->
        console.error err
        notify err.message

    storeHistory: (step) -> @nextStep step, true

    stampStep: (step) ->
      {Q, serviceStamp} = @
      if not step.data.service? # Nothing to stamp.
        Q.when step
      else
        serviceStamp(step.data.service).then (stamp) -> L.assign {stamp}, step

    nextStep: (step, silently = false) =>
      {Histories, scope, currentCardinal, console, location, silentLocation} = @
      console.debug "Next step:", step
      console.log "storing - silently? #{ silently }"
      nextCardinal = currentCardinal + 1
      appended = null

      goTo = if silently
        (url) =>
          @params.idx = nextCardinal
          silentLocation.silent url
          @currentCardinal = nextCardinal
          @init()
      else
        (url) -> location.url url

      history = scope.history

      @stampStep(step).then (stamped) ->
        console.log 'STAMPED', stamped
        if history.steps.length isnt currentCardinal
          title = "Fork of #{ history.title }"
          console.debug "Forking at #{ currentCardinal } as #{ title }"
          fork = Histories.fork {id: history.id, at: currentCardinal, title}, ->
            console.debug "Forked history"
            appended = Histories.append {id: fork.id}, stamped, ->
              console.debug "Created step #{ appended.id }"
              goTo "/history/#{ fork.id }/#{ nextCardinal }"
        else
          appended = Histories.append {id: history.id}, stamped, ->
            console.debug "Created step #{ appended.id }"
            goTo "/history/#{ history.id }/#{ nextCardinal }"
            ga 'send', 'event', 'history', 'append', step.tool

