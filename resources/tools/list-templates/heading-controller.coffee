define ['lodash', './dialogue', 'text!./template-dialogue.html'], (L, Ctrl, View) ->

  controller = (console, scope, Modals, Q, connectTo) ->
    
    console.log scope.data
    scope.listName = scope.data.name
    scope.ids = scope.data.ids
    scope.type = scope.data.type
    scope.service = root: scope.data.root ? scope.data.service.root

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
          title: "Ran #{ selectedTemplate.name } over #{ scope.listName }"
          tool: 'show-table'
          data:
            service:
              root: scope.service.root
            query: selectedTemplate
        scope.appendStep data: step

  ['$log', '$scope', '$modal', '$q', 'connectTo', controller]

