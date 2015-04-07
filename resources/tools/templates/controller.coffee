define ['angular', 'lodash', 'app', 'imjs'], (ng, L, app, {Service}) ->

  app.filters.register 'templateTitle', getTemplateTitle = -> ({title}) ->
    title.replace(/-->/g, '\u21E8').replace(/<--/g, '\u21E6')

  app.filters.register 'filterTemplates', Array '$filter', (filters) -> (ts, term, conns) ->
    ngFilter = filters 'filter'
    ts?.filter (t) -> # We serialise first so ng doesn't blow the stack
      return false if conns[t.service.root].disabled
      (ngFilter [L.merge t.toJSON(), service: t.service.name], term).length

  app.filters.register 'fromService', -> (xs, service) ->
    xs?.filter (x) -> x.service.name is service.name

  OTHER_SWITCH_STATE =
    ON: 'OFF'
    OFF: 'ON'

  hasLookupConstraints = (q) -> q.constraints.some (c) -> c.op is "LOOKUP"
  lookupConstraints = (q) -> q.constraints.filter (c) -> c.op is "LOOKUP"

  effectiveTemplate = (q) ->
    return q unless hasLookupConstraints q
    r = q.clone()
    for c, i in r.constraints when q.constraints[i].replaceWithList
      c.op = 'IN'
      c.value = q.constraints[i].list.name
    return r

  app.controllers.register 'TemplateListRowController',

    Array '$scope', '$q', class TemplateListRowController

      switchConstraint: (con) ->
        con.switched = OTHER_SWITCH_STATE[con.switched]

      compatibleLists: (path) ->
        pathInfo = @template.makePath path
        @_lists_cache[path] ?= @lists.filter (l) -> pathInfo.isa l.type

      runTemplate: ->
        @_scope.$emit 'start-history',
          verb:
            ed: "ran"
            ing: "running"
          thing: "#{ getTemplateTitle() @template } template query"
          tool: 'show-table',
          data:
            service:
              root: @template.service.root,
            query: (effectiveTemplate @template)

      lists: []

      constructor: (scope, Q) ->

        @_lists_cache = {}
        @_count_cache = {}
        @_scope = scope

        scope.formattedPaths = {}

        scope.$watch 'template', => @template = scope.template

        if hasLookupConstraints scope.template
          Q.when(scope.template.service.fetchLists()).then (lists) =>
            @lists = lists
            @_lists_cache = {}
            for c in lookupConstraints(scope.template)
              c.list = @compatibleLists(c.path)[0]

        updateCount = => # Only run the query if selected.
          return unless scope.template.selected
          t = effectiveTemplate scope.template
          Q.when(@_count_cache[t.toXML()] ?= t.count()).then (c) => scope.results = c

        scope.$watch ((s) -> effectiveTemplate(s.template).toXML()), updateCount

        scope.$watch 'template.selected', updateCount

        scope.$watch 'template.selected', (selected) -> if selected
          setName = (path) -> (name) -> scope.formattedPaths[path] = name
          t = scope.template
          for p in t.views.concat(c.path for c in t.constraints)
            getName = t.makePath(p).getDisplayName()
            getName.then setName p

  validPath = (model, path) ->
    try
      model.makePath path
      path
    catch e
      false

  isBetween = (inputType, outputType) -> (template) ->
    inputType = validPath template.model, inputType?.className
    outputType = validPath template.model, outputType?.className
    ok = true
    if inputType
      ok and= L.some template.constraints, ({editable, path}) ->
        path = template.makePath(path)
        if path.isAttribute()
          path = path.getParent()
        editable and path.isa(inputType)
    if outputType
      ok and= L.some template.views, (path) ->
        path = template.makePath(path).getParent()
        path.isa(outputType)
    ok

  typeWatcher = ({inputType, outputType}) ->
    inputType?.className + outputType?.className

  # Need to json-ify data for presentation.
  # templateData = ({constraints, views, title, name, description, constraintLogic}) ->
  #  {constraints, views, title, name, description, constraintLogic}

  filterTemplates = ({templates, inputType, outputType}) ->
    f = isBetween inputType, outputType
    (t for t in (templates ? []) when f t)

  CLASSES_TO_IGNORE = ['java.lang.Object', 'InterMineObject']

  class TemplatesList

    @$inject = [
      '$scope', '$log', '$q', '$filter', '$location',
      'connectToAll', 'getMineUserEntities', '$ngSilentLocation',
      '$routeParams'
    ]

    toggleSelected: (template) ->
      if template is @template
        delete @template
        @silentLoc.silent "/starting-point/templates"
      else
        @template = template
        service = template.service.name
        name = template.name
        template.selected = true
        @silentLoc.silent "/starting-point/templates/#{ service }/#{ name }"

    restoreSelectedTemplate: (qs, params) ->
      [service, name] = (params.args?.split('/') ? [])
      console.log 'looking for ', service, name, params
      if service and name
        @template = L.find qs, (q) ->
          (q.service.name is service) and (q.name is name)
        if @template
          @template.selected = true
        else
          @silentLoc.silent "/starting-point/templates"

    constructor: (scope, @log, Q, filters, @location, connect, getMineUserEntities, @silentLoc, routeParams) ->
      console.log "Loading templates"
      scope.defaults = {}
      scope.classes = []
      scope.connections = {}
      scope.inputType = scope.outputType = null

      scope.formattedPaths = {}

      connecting = connect()
      gettingEntitites = getMineUserEntities()

      # Make the connections available to the template.
      connecting.then (cs) -> scope.connections = cs

      updateTemplates = -> scope.suitableTemplates = filterTemplates scope

      # Load all templates.
      Q.all [connecting, gettingEntitites]
       .then ([cs, es]) -> (cs[e.source].query(e.entity) for e in es.templates)
       .then Q.all      #  ^-- A list of promises, therefore it needs unpacking.
       .then (ts) -> scope.templates = ts
       .then updateTemplates
       .then => @restoreSelectedTemplate(scope.templates, routeParams)

      scope.$on 'reset', (evt) ->
        scope.outputType = scope.defaults.outputType
        scope.inputType = scope.defaults.inputType
        scope.templateFilter = null

      connecting.then (connections) ->
        getModels = Q.all(s.fetchModel() for s in L.uniq(L.values(connections)))
        classes = {}
        getModels.then (models) -> L.each models, (m) ->
          L.each m.classes, (cld, name) ->
            return if classes[name] or name in CLASSES_TO_IGNORE
            classes[name] = className: name
            m.makePath(name).getDisplayName().then (displayName) ->
              classes[name].displayName = displayName
        scope.classes = L.values classes

      scope.$watch typeWatcher, updateTemplates

  return TemplatesList

