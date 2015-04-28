define (require) ->

  ng = require 'angular'
  L = require 'lodash'
  require 'services'

  StartingPoints = ng.module 'steps.starting-point.controller', ['steps.services']

  class StartingPointCtrl

    @$inject = ['$scope', '$routeParams', 'historyListener']

    constructor: (scope, params, historyListener) ->

      historyListener.watch scope

      byIdent = ({ident}) -> ident is params.tool
      if params.service?
        filter = (sp) -> byIdent(sp) and params.service is sp.args?.service
      else
        filter = byIdent

      # Wait for the starting points to be loaded by the application.
      scope.$watch 'startingPoints', (startingPoints) -> if startingPoints
        tool = L.find startingPoints, filter
        if tool
          scope.tool = Object.create tool
          scope.tool.state = 'FULL'

  StartingPoints.controller 'StartingPointCtrl', StartingPointCtrl
