define ['angular', 'lodash', 'app', 'imjs'], (ng, L, {filters}, {Service}) ->

  filters.register 'templateTitle', -> ({title}) ->
    title.replace(/-->/g, '\u21E8').replace(/<--/g, '\u21E6')

  injectables = ['$scope', '$log', '$timeout', '$q', '$filter', 'Mines', 'ClassUtils']

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
  templateData = ({constraints, views, title, name, description, constraintLogic}) ->
    {constraints, views, title, name, description, constraintLogic}

  filterTemplates = ({templates, model, inputType, outputType}) ->
    f = isBetween model, inputType, outputType
    if templates and model then (templateData t for t in templates when f t) else []

  return Array injectables..., (scope, log, timeout, Q, filters, Mines, ClassUtils) ->
    scope.defaults = {}

    scope.formattedPaths = {}

    scope.$watch 'templates', ->
      setName = (path) -> (name) -> timeout -> scope.formattedPaths[path] = name
      for t in scope.templates
        for v in t.views
          getName = t.makePath(v).getDisplayName()
          getName.then setName v
        for c in t.constraints
          getName = t.makePath(c.path).getDisplayName()
          getName.then setName c.path

    setTemplates = ({query}) -> (ts) ->
      Q.all(query t for _, t of ts).then (qs) -> timeout ->
        scope.templates = qs
        scope.suitableTemplates = filterTemplates scope

    scope.$on 'reset', (evt) ->
      scope.outputType = scope.defaults.outputType
      scope.inputType = scope.defaults.inputType
      scope.templateFilter = null

    scope.runQuery = (q) ->
      scope.$emit 'start-history',
        verb:
          ed: "ran"
          ing: "running"
        thing: "#{ filters('templateTitle')(q) } template query"
        tool: 'show-table',
        data:
          service:
            root: scope.connection.root,
          query: q

    scope.classes = []
    scope.inputType = scope.outputType = scope.serviceName = ''

    fetchingDefaultMine = Mines.get 'default'

    fetchingDefaultMine.then ({name}) -> timeout -> scope.serviceName = name

    connecting = fetchingDefaultMine.then Service.connect

    connecting.then (connection) -> scope.connection = connection

    connecting.then (connection) -> connection.fetchModel().then ClassUtils.setClasses scope

    connecting.then (connection) -> connection.fetchTemplates().then setTemplates connection

    updateTemplates = -> scope.suitableTemplates = filterTemplates scope
    typeWatcher = ({inputType, outputType}) -> inputType?.className + outputType?.className

    scope.$watch typeWatcher, updateTemplates

    scope.$watch 'serviceName', (name) -> scope.tool.heading += " in #{ name }" if name

