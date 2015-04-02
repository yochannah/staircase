define ['angular', 'lodash', 'app', 'imjs'], (ng, L, app, {Service}) ->

  app.filters.register 'templateTitle', getTemplateTitle = -> ({title}) ->
    title.replace(/-->/g, '\u21E8').replace(/<--/g, '\u21E6')

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

          updateCount = =>
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

  isBetween = (model, inputType, outputType) ->
    inputType = validPath model, inputType?.className
    outputType = validPath model, outputType?.className
    (template) ->
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

  # Need to json-ify data for presentation.
  # templateData = ({constraints, views, title, name, description, constraintLogic}) ->
  #  {constraints, views, title, name, description, constraintLogic}

  filterTemplates = ({templates, model, inputType, outputType}) ->
    f = isBetween model, inputType, outputType
    # if templates and model then (templateData t for t in templates when f t) else []
    if templates and model then (t for t in templates when f t) else []

  class TemplatesList

    @$inject = [
      '$scope', '$log', '$timeout', '$q', '$filter', '$location',
      'connect', 'ClassUtils'
    ]

    toggleSelected: (template) ->
      if template.title is @template?.title
        @location.search 'title', null
      else
        @location.search 'title', template.title

    restoreSelectedTemplate: (qs, searchParams) ->
      @template = L.findWhere qs, L.pick searchParams, 'title', 'name'
      @template.selected = true if @template?

    constructor: (scope, @log, timeout, Q, filters, @location, connect, ClassUtils) ->
      searchParams = @location.search()
      scope.defaults = {}
      scope.classes = []
      scope.inputType = scope.outputType = scope.serviceName = ''

      scope.formattedPaths = {}

      connecting = connect 'default'

      setTemplates = ({query}) => (ts) =>
        Q.all(query t for _, t of ts).then (qs) =>
          scope.templates = qs
          scope.suitableTemplates = filterTemplates scope
          @restoreSelectedTemplate qs, searchParams

      scope.$on 'reset', (evt) ->
        scope.outputType = scope.defaults.outputType
        scope.inputType = scope.defaults.inputType
        scope.templateFilter = null

      connecting.then ({name}) -> scope.serviceName = name

      connecting.then (connection) ->
        connection.fetchModel().then ClassUtils.setClasses scope

      connecting.then (connection) ->
        connection.fetchTemplates().then setTemplates connection

      updateTemplates = -> scope.suitableTemplates = filterTemplates scope

      typeWatcher = ({inputType, outputType}) -> inputType?.className + outputType?.className

      scope.$watch typeWatcher, updateTemplates

      scope.$watch 'serviceName', (name) ->
        scope.tool.heading += " in #{ name }" if name

  return TemplatesList

