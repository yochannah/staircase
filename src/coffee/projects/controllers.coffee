define (require, exports) ->

  L = require 'lodash'
  ng = require 'angular'
  require './services'

  TRIM_RE = /(^\s*|\s*$)/g
  trim = (s) -> s?.replace TRIM_RE, ''

  class ProjectsCtrl

    @$inject = ['Mines', 'Projects', 'getMineUserEntities']

    showExplorer: false

    dateFormat: 'dd/MM/yyyy hh:mm a'

    constructor: (Mines, @Projects, getEntities) ->

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

      @sync()

    getHref: ({source, item_type, item_id}) ->
      switch item_type
        when 'List'
          "starting-point/choose-list/#{ source }?name=#{ item_id }"

    isEmptyProject: ({child_nodes, contents}) ->
      (not child_nodes?.length) and (not contents?.length)

    checkEmpty: (value) -> "Please provide a value" unless trim value

    goToRoot: ->
      @pathToHere = []
      @currentProject = @getRoot()

    getRoot: ->
      _is_empty: (@allProjects.length > 0)
      contents: []
      child_nodes: @allProjects

    # Go back up the path a ways, display the new root.
    goToPathSegment: (idx) ->
      @pathToHere = @pathToHere.slice 0, (idx + 1)
      @setCurrentProjectFromPath()

    # Fetch the project graph, and then re-select the current project.
    sync: -> @allProjects = @Projects.all => @setCurrentProjectFromPath()

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
      @pathToHere = @pathToHere.concat [{id: project.id, name: project.title}]
      @currentProject = project
      @currentProject._is_empty = @isEmptyProject project

    updateProject: (project) ->
      # Only send the project info we are updating.
      data = L.pick project, 'title', 'description'
      @Projects.update {projectId: project.id}, data, => @sync()

    deleteProject: (project) ->
      @Projects.delete {projectId: project.id}, => @sync()

    dropped: (pkg, dest) ->
      pkg = JSON.parse pkg if L.isString pkg
      dest = JSON.parse dest if L.isString dest
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
      if projectId # Add as a nested project.
        @Projects.addItem {projectId}, data, done
      else # Add as top level project.
        @Projects.create data, done
      
  ng.module('steps.projects.controllers', ['steps.services', 'steps.projects.services'])
    .controller 'ProjectsCtrl', ProjectsCtrl

