define ['lodash'], (L) ->
  Array '$rootScope', '$scope', '$http', '$location', 'startHistory', (root, scope, http, location, startHistory) ->
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

    startHistory scope

    scope.$apply()

