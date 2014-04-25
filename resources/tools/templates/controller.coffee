define ['angular', 'lodash', 'app', 'imjs'], (ng, L, {filters}, {Service}) ->

  filters.register 'templateTitle', -> ({title}) ->
    title.replace(/-->/g, '\u21E8').replace(/<--/g, '\u21E6')

  injectables = ['$scope', '$log', '$timeout', '$q', 'Mines']

  validPath = (model, path) ->
    try
      model.makePath path
      path
    catch e
      false

  isBetween = (model, inputType, outputType) ->
    inputType = validPath model, inputType
    outputType = validPath model, outputType
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

  filterTemplates = ({templates, model, inputType, outputType}) ->
    if templates and model
      f = isBetween model, inputType, outputType
      (t for t in templates when f t)

  return Array injectables..., (scope, log, timeout, Q, Mines) ->

    setClasses = (scope) -> (model) -> timeout ->
      scope.model = model
      scope.classes = Object.keys model.classes
      scope.outputType = switch model.name
        when 'genomic' then 'Gene'
        when 'testmodel' then 'Employee'

    setTemplates = ({query}, scope) -> (ts) ->
      Q.all(query t for _, t of ts).then (qs) -> timeout ->
        scope.templates = qs
        scope.filteredTemplates = filterTemplates scope

    scope.classes = []
    scope.inputType = scope.outputType = scope.serviceName = ''

    scope.runQuery = (q) -> log.info "Results of #{ q.title } please"

    fetchingDefaultMine = Mines.get 'default'

    fetchingDefaultMine.then ({ident}) -> timeout -> scope.serviceName = ident

    connecting = fetchingDefaultMine.then Service.connect

    connecting.then (connection) -> connection.fetchModel().then setClasses scope

    connecting.then (connection) -> connection.fetchTemplates().then setTemplates connection, scope

    scope.$watch (({inputType, outputType}) -> inputType + outputType), ->
      scope.filteredTemplates = filterTemplates scope


