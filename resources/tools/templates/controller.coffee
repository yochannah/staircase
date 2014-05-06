define ['angular', 'lodash', 'app', 'imjs'], (ng, L, {filters}, {Service}) ->

  filters.register 'templateTitle', -> ({title}) ->
    title.replace(/-->/g, '\u21E8').replace(/<--/g, '\u21E6')

  injectables = ['$scope', '$log', '$timeout', '$q', 'Mines', 'ClassUtils']

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

  templateData = ({constraints, views, title, name, description, constraintLogic}) ->
    {constraints, views, title, name, description, constraintLogic}

  filterTemplates = ({templates, model, inputType, outputType}) ->
    if templates and model
      f = isBetween model, inputType, outputType
      (templateData t for t in templates when f t) # Need to json-ify data for presentation.

  return Array injectables..., (scope, log, timeout, Q, Mines, ClassUtils) ->
    scope.defaults = {}

    setTemplates = ({query}) -> (ts) ->
      Q.all(query t for _, t of ts).then (qs) -> timeout ->
        scope.templates = qs
        scope.filteredTemplates = filterTemplates scope

    scope.$on 'reset', (evt) ->
      scope.outputType = scope.defaults.outputType
      scope.inputType = scope.defaults.inputType
      scope.templateFilter = null

    scope.runQuery = (q) ->
      scope.$emit 'start-history',
        title: "Ran #{ q.title } template query",
        tool: '/tools/show-table',
        data:
          url: scope.connection.root,
          token: scope.connection.token,
          query: q

    scope.classes = []
    scope.inputType = scope.outputType = scope.serviceName = ''

    fetchingDefaultMine = Mines.get 'default'

    fetchingDefaultMine.then ({ident}) -> timeout -> scope.serviceName = ident

    connecting = fetchingDefaultMine.then Service.connect

    connecting.then (connection) -> scope.connection = connection

    connecting.then (connection) -> connection.fetchModel().then ClassUtils.setClasses scope

    connecting.then (connection) -> connection.fetchTemplates().then setTemplates connection

    updateTemplates = -> scope.filteredTemplates = filterTemplates scope
    typeWatcher = ({inputType, outputType}) -> inputType?.className + outputType?.className

    scope.$watch typeWatcher, updateTemplates

