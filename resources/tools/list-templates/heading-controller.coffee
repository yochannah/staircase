define ['lodash', './dialogue', 'text!./template-dialogue.html', './template-controller'], (L, Ctrl, View, tc) ->

  controller = (console, scope, Modals, Q, connectTo) ->

    scope.listName = scope.data.name
    scope.ids = scope.data.ids
    scope.type = scope.data.type
    scope.service = root: scope.data.root ? scope.data.service.root
    connect = connectTo scope.service.root
    scope.listnames = []
    scope.templatecontroller = tc

    getParsedTitle = ({title, name}) -> (title or name).replace /.*--> /, ''

    if scope.listName?
      connect.then (s) -> s.fetchTemplates().then (ts) ->
        limit = 0
        for listname, values of ts
            if "im:aspect:#{scope.category.label}" in values.tags
              if limit < 5
                do (scope, listname, values) ->
                  values.readable = getParsedTitle values
                  scope.listnames.push values
                  do (values) ->
                    s.count(values).then (res) ->
                      scope.$apply -> values.count = res
              limit++
    scope.runTemplate = (selectedTemplate) ->
      step =
        title: "Structured Search"
        description: "Using List #{ selectedTemplate.name } over #{ scope.listName }"
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

      modalInstance.result.then (selectedTemplate) ->
        step =
          title: "Structured Search"
          description: "Using List #{ selectedTemplate.name } over #{ scope.listName }"
          tool: 'show-table'
          data:
            service:
              root: scope.service.root
            query: selectedTemplate
        scope.appendStep data: step

  ['$log', '$scope', '$modal', '$q', 'connectTo', controller]
