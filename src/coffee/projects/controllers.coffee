define (require, exports) ->

  L = require 'lodash'
  ng = require 'angular'
  require './services'
  require 'angular-silent'

  TRIM_RE = /(^\s*|\s*$)/g
  trim = (s) -> s?.replace TRIM_RE, ''

  class ProjectCreator

    @$inject = ['$scope']

    constructor: (@scope, @Projects) ->
      @scope.newProjectName = null
      @scope.newProjectDesc = null

    create: ->
      name = @scope.newProjectName
      desc = @scope.newProjectDesc
      @scope.appView.createProject(name, desc)
      @scope.newProjectName = null
      @scope.newProjectDesc = null

  class ProjectsCtrl

    @$inject = [
      '$routeParams',
      '$ngSilentLocation',
      'Mines',
      'Projects',
      'getMineUserEntities'
    ]

    showExplorer: false

    dateFormat: 'dd/MM/yyyy hh:mm a'

    tableSort: 'title'

    constructor: (params, @silentLocation, Mines, @Projects, getEntities) ->
      console.log params

      # The kinds of things we can add to projects.
      @templates = []
      @lists = []

      # The project browser model - a store of projects,
      # the current project, and the path to the current project (made
      # up of path segments {name, id}
      @allProjects = []
      @pathToHere = []
      @currentProject = _is_empty: true, child_nodes: [], contents: []

      # The data sources we have available to us.
      Mines.all().then (mines) => @mines = mines
      # The things we can add to projects.
      getEntities().then ({templates, lists}) =>
        @templates = templates
        @lists = lists

      @sync().then =>
        paramPath = params.pathToHere?.split '/'
        if paramPath?.length
          @pathToHere = @createPathFromTitles paramPath
          @setCurrentProjectFromPath()

    createPathFromTitles: (titles) ->
      path = []
      here = @currentProject
      for t in titles
        next = L.findWhere here.child_nodes, title: t
        return path unless next
        path.push id: next.id, name: t
        here = next
      return path

    getHref: ({source, item_type, item_id}) ->
      switch item_type
        when 'List'
          "starting-point/choose-list/#{ source }?name=#{ item_id }"

    isEmptyProject: ({child_nodes, contents}) ->
      (not child_nodes?.length) and (not contents?.length)

    checkEmpty: (value) -> "Please provide a value" unless trim value

    selectedItems: ->
      L.where(@lists, selected: true).concat(L.where @templates, selected: true)

    addAllSelected: ->
      currentId = @currentProject.id
      for item in @selectedItems()
        @addItem item, currentId
        item.selected = false

    goToRoot: ->
      @pathToHere = []
      @currentProject = @getRoot()
      @silentLocation.silent '/projects'

    getRoot: ->
      _is_empty: (@allProjects.length is 0)
      contents: []
      child_nodes: @allProjects

    # Go back up the path a ways, display the new root.
    goToPathSegment: (idx) ->
      @pathToHere = @pathToHere.slice 0, (idx + 1)
      @setCurrentProjectFromPath()
      @updateURL()

    updateURL: ->
      url = "/projects/#{ (s.name for s in @pathToHere).join '/' }"
      @silentLocation.silent url

    # Fetch the project graph, and then re-select the current project.
    sync: ->
      @allProjects = @Projects.all => @setCurrentProjectFromPath()
      @allProjects.$promise

    setCurrentProjectFromPath: ->
      here = @getRoot()
      # Walk the path to find where we are now, setting names as we go.
      for segment, idx in @pathToHere
        here = L.findWhere here.child_nodes, id: segment.id
        if here? # Cool, found it.
          segment.name = here.title # Update in case we are in sync.
        else if idx is 0 # Uh, oh, nothing.
          return @goToRoot()
        else # Deletion? Go to last good node.
          return @goToPathSegment idx - 1

      @currentProject = here
      @currentProject._is_empty = @isEmptyProject here

    # Add a project to the path, and focus on it.
    setCurrentProject: (project) ->
      unless L.findWhere(@currentProject.child_nodes, id: project.id)
        @pathToHere = [] # replacement, not append.
      @pathToHere = @pathToHere.concat [{id: project.id, name: project.title}]
      @currentProject = project
      @currentProject._is_empty = @isEmptyProject project
      @updateURL()

    updateProject: (project) ->
      # Only send the project info we are updating.
      data = L.pick project, 'title', 'description'
      @Projects.update {projectId: project.id}, data, => @sync()

    deleteProject: (project) ->
      @Projects.delete {projectId: project.id}, => @sync()

    deleteItem: (project, item) ->
      @Projects.removeItem {projectId: project.id, itemId: item.id}, => @sync()

    dropped: (pkg, dest) ->
      pkg = JSON.parse pkg if L.isString pkg
      dest = JSON.parse dest if L.isString dest
      console.log "DROP", pkg, dest
      return unless dest.type is 'Project'
      @addItem pkg, dest.id
    
    addItem: (thing, projectId) ->
      projectId ?= @currentProject.id
      item =
        type: 'Item'
        source: thing.source
        item_type: thing.type
        item_id: thing.id

      if not projectId
        @error = 'NO_CURRENT_PROJECT'
      else if thing.type not in ['List', 'Template']
        @error = 'UNKNOWN_ITEM_TYPE'
      else
        @Projects.addItem {projectId}, item, => @sync()

    createProject: (title, description) ->
      data = {title, description, type: 'Project'}
      projectId = @currentProject.id
      done = => @sync()
      ret = if projectId # Add as a nested project.
        @Projects.addItem {projectId}, data, done
      else # Add as top level project.
        @Projects.create data, done
      ret.$promise.then null, (err) => @error = err.data
      
  ng.module('steps.projects.controllers', ['steps.services', 'steps.projects.services'])
    .controller 'ProjectsCtrl', ProjectsCtrl
    .controller 'ProjectCreator', ProjectCreator

