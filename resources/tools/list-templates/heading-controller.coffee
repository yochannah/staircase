define ['lodash', './dialogue', 'text!./template-dialogue.html', './template-controller', './helpers'], (L, Ctrl, View, tc, H) ->

  controller = (console, scope, Modals, Q, connectTo) ->

    scope.listName = scope.data.name
    scope.ids = scope.data.ids
    scope.type = scope.data.type
    scope.service = root: scope.data.root ? scope.data.service.root
    connect = connectTo scope.service.root
    scope.listnames = []
    scope.TemplateController = tc

    scope.descLimit = 120 # characters. Tweet sized is best.
    scope.service = root: scope.data.root ? scope.data.service.root
    connect = connectTo scope.service.root
    model = connect.then (s) -> s.fetchModel()
    templates = connect.then (s) -> s.fetchTemplates()

    if scope.listName?
      scope.list = scope.data
      scope.items = null
    else if scope.ids?
      scope.list = null
      scope.items = {ids: scope.data.ids, type: scope.type}

    Q.all([connect, model, templates]).then ([service, model, templates]) ->

      scope.service = service

      isSuitable = H.isSuitableForType (scope.list ? scope.items).type, model

      templates = L.values templates
      for template in templates
        template.parsedTitle = H.getParsedTitle template
      suitable = templates.filter isSuitable

      filtered = L.filter suitable, (tpl) ->
        if "im:aspect:#{scope.category.label}" in tpl.tags then return true

      # Take just five
      scope.templates = filtered.slice 0, 5

    scope.run = (selectedTemplate) ->
      if scope.ids?
        over = "#{scope.ids.length} #{scope.type}s"
      else if scope.listName?
        over = scope.listName

      parsed = H.getParsedTitle selectedTemplate

      step =
        title: "Ran Template"
        description: "Using template #{ parsed } over " + over
        tool: 'show-table'
        data:
          service:
            root: scope.service.root
          query: selectedTemplate
      scope.appendStep data: step

    scope.showTemplates = ->
      console.log "showing templates at #{ scope.service.root }"

      connect = connectTo scope.service.root
      injected =
        model: -> connect.then (s) -> s.fetchModel()
        templates: -> connect.then (s) -> s.fetchTemplates()
        service: -> connect

      if scope.listName?
        injected.list = -> connect.then (s) -> s.fetchList scope.listName
        injected.items = -> null
      else if scope.ids?
        injected.list = -> null
        injected.items = -> {ids: scope.ids, type: scope.type}

      modalInstance = Modals.open
        template: View
        controller: Ctrl
        size: 'lg'
        resolve: injected




  ['$log', '$scope', '$modal', '$q', 'connectTo', controller]
