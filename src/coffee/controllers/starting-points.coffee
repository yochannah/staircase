define ['lodash'], (L) ->
  Array '$rootScope', '$scope', '$http', '$location', 'Histories', (root, scope, http, location, Histories) ->
    root.startingPoints = []

    http.get("/tools", {params: {capabilities: "initial"}})
        .then ({data}) -> root.startingPoints = data

    scope.expandTool = (tool) ->
      for other in scope.startingPoints when other isnt tool
        other.state = 'DOCKED'
      tool.state = 'FULL'

    scope.getHeightClass = ({state, tall}) ->
      if state is 'FULL'
        'full-height'
      else if tall
        'double-height'
      else
        ''

    scope.getWidthClass = ({state, width}) ->
      if state is 'FULL'
        'col-xs-12'
      else
        "col-lg-#{ 3 * width } col-md-#{ 4 * width } col-sm-#{ Math.min(12, 6 * width) }"

    scope.undockAll = ->
      for tool in scope.startingPoints
        tool.state = null

    scope.anyToolDocked = -> L.some scope.startingPoints, state: 'DOCKED'

    scope.$on 'start-history', (evt, step) ->
      history = Histories.create {title: "Un-named history"}, ->
        step = Histories.append {id: history.id}, step, ->
          console.log("Created history " + history.id + " and step " + step.id)
          location.url "/history/#{ history.id }/1"

    scope.$apply()

