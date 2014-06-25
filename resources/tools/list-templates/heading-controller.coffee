define ['lodash', './dialogue', 'text!./template-dialogue.html'], (L, Ctrl, View) ->

  controller = (scope, Modals, connectTo) ->
      
      console.log scope.data
      scope.listName = scope.data.name

      scope.showTemplates = ->
        connecting = connectTo(scope.data.root)
        modalInstance = Modals.open
          template: View
          controller: Ctrl
          size: 'lg'
          resolve:
            list: -> connecting.then (s) -> s.fetchList scope.listName
            model: -> connecting.then (s) -> s.fetchModel()
            templates: -> connecting.then (s) -> s.fetchTemplates()

        modalInstance.result.then (selectedTemplate) ->
          step =
            title: "Ran #{ selectedTemplate.name } over #{ scope.listName }"
            tool: 'show-table'
            data:
              service:
                root: scope.data.root
              query: selectedTemplate
          scope.appendStep data: step

  ['$scope', '$modal', 'connectTo', controller]

