define ['angular', 'lodash', 'imjs', 'angular-cookies', 'services'], (ng, L, imjs) ->

  Controllers = ng.module('steps.controllers', ['ngCookies', 'steps.services'])

  requiredController = (ident) -> Array '$scope', '$injector', ($scope, injector) ->
    require ['controllers/' + ident], (ctrl) ->
      instance = injector.instantiate(ctrl, {$scope})
      $scope.controller = instance # Working around breakage of controllerAs
      $scope.foo = 1
      $scope.$apply()

  mountController = (name, ident) -> Controllers.controller name, requiredController ident

  Controllers.controller 'FooterCtrl', Array '$scope', '$cookies', (scope, cookies) ->
    scope.showCookieMessage = cookies.ShowCookieWarning isnt "false"

    scope.$watch "showCookieMessage", -> cookies.ShowCookieWarning = String scope.showCookieMessage

  Controllers.controller 'WelcomeCtrl', Array '$scope', (scope) -> scope.showWelcome = true

  Controllers.controller 'IndexCtrl', ->

  Controllers.controller 'StartingPointCtrl', Array '$scope', '$routeParams', 'historyListener', (scope, params, historyListener) ->

    historyListener.watch scope

    byIdent = ({ident}) -> ident is params.tool
    if params.service?
      filter = (sp) -> byIdent(sp) and params.service is sp.args?.service
    else
      filter = byIdent

    scope.$watch 'startingPoints', (startingPoints) -> if startingPoints
      tool = L.find startingPoints, filter
      if tool
        scope.tool = Object.create tool
        scope.tool.state = 'FULL'

  Controllers.controller 'AboutCtrl', Array '$http', '$scope', 'historyListener', (http, scope, historyListener) ->
    http.get('/tools', {params: {capabilities: 'initial'}})
        .then ({data}) ->
          scope.tool = L.chain(data).where(width: 1).find('description').value()
          scope.tool.expandable = false

    historyListener.watch scope

  #############################################
  ### PROJECTS CONTROLLER
  #############################################
  Controllers.controller 'ProjectsCtrl', Array '$rootScope', '$modal', '$scope', 'Mines', 'Projects', 'uuid', 'Persona', (rs, modals, scope, Mines, Projects, uuid, Persona) ->

    # Does an initial synch at the bottom of the controller.

    scope.breadcrumbs = []

    scope.status =
      isopen: false

    scope.level = []
    scope.flattened = []

    scope.getformname = (project) ->
      "folder" + project.id

    scope.updatefolder = (folderdata) ->
      Projects.update folderdata.id, folderdata
      .then (results) ->
        console.log "post update is", results

    scope.checkempty = (value) ->
      if value is "" or value is null then return "Please provide a value."

    scope.getname = (id) ->
      found = L.findWhere scope.flattened, {id: id}
      found.title

    scope.geticon = (obj) ->
      switch obj.type
        when "Project" then return "fa fa-folder"
        when "List" then return "fa fa-list"
        when "Query" then return "fa fa-search"

    scope.deletefolder = (proj) ->
      console.log "delete the folder", proj
      Projects.deletefolder proj.id
      .then ->
        do synch

    scope.deleteitem = (item) ->
      Projects.deleteitem item
      .then (results) ->
        do synch

    synch = ->

      look = (item) ->
        if not scope.level.id?
          scope.level = scope.allProjects
        else
          if item.child_nodes.length > 0
            for folder in item.child_nodes
              if folder.id is scope.level.id
                scope.level = folder
              else
                look folder

      flatten = (item) ->
        scope.flattened.push item
        if item.child_nodes.length > 0
          for folder in item.child_nodes
            flatten folder

      Projects.all().then (projects)->
        scope.allProjects = projects
        look scope.allProjects

        # Used for quick retrieval and breadcrumbs
        scope.flattened = []
        flatten scope.allProjects



    scope.dropped = (pkg, dest) ->

      if typeof pkg is "string" then pkg = JSON.parse pkg
      if typeof dest is "string" then dest = JSON.parse dest


      # This needs a serious overhaul. Proof of concept, for now:

      # Are we adding a project to a list?
      if pkg.type is "List" and dest.type is "Project"
        Projects.addto dest.id, pkg
          .then (result) ->         
              do synch

    scope.setlevelbc = (id, index) ->
      if !items? and !index?
        scope.level = scope.allProjects
      else
        # Find the object in our
        found = L.findWhere scope.flattened, {id: id}
        scope.level = L.findWhere scope.flattened, {id: id}

      scope.breadcrumbs = scope.breadcrumbs.slice 0, index + 1

    scope.setlevel = (items) ->
      scope.level = items
      scope.breadcrumbs.push scope.level.id

    scope.hoverIn = () ->
      this.hoverEdit = true

    scope.hoverOut = () ->
      this.hoverEdit = false

    scope.setInspection = (item) ->
      scope.inspection = item

    scope.createProject = (title) ->
      data =
        title: title
        parent_id: scope.level.id

      scope.foldername = ""

      Projects.put(data)
      .then (result) ->
        do synch

    Mines.all().then (values) ->

      scope.lists = []

      values.forEach (amine) ->
        service = imjs.Service.connect root: amine.root
        service.fetchLists().then (lists) =>

          lists = lists[0..5]
          for list in lists
              list.type = "List"
              list.id = list.title
              list.short = amine.name

          scope.lists = scope.lists.concat lists
          scope.$digest()

    do synch

    # Poll this to retrieve user's projects!
    # setTimeout ->
    #   console.log "IDENT1", scope.auth
    # , 3000

  # Inline controller.
  Controllers.controller 'AuthController', Array '$rootScope', '$scope', 'Persona', (rs, scope, Persona) ->
    rs.auth ?= identity: null # TODO: put this somewhere more sensible.

    changeIdentity = (identity) ->
      scope.auth.identity = identity
      scope.auth.loggedIn = identity?

    scope.persona = new Persona {changeIdentity, loggedInUser: scope.auth.identity}

  Controllers.controller 'QuickSearchController', Array '$log', '$scope', 'historyListener', (log, scope, {startHistory}) ->

    scope.startQuickSearchHistory = (term) ->
      startHistory
        thing: "for #{ term }"
        verb:
          ed: "searched"
          ing: "searching"
        tool: "keyword-search"
        data:
          searchTerm: term

  mountController 'StartingPointsController', 'starting-points'

  mountController 'HistoryStepCtrl', 'history-step'

  mountController 'HistoryCtrl', 'history'

  mountController 'BrandCtrl', 'brand'

  mountController 'NavCtrl', 'brand'

  mountController 'FacetCtrl', 'facets'

  return Controllers

