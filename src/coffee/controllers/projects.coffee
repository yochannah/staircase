define (require, module, exports) ->

  L = require 'lodash'

  class ProjectsController

    @$inject = ['$rootScope', '$modal', '$scope', 'Mines', 'Projects', 'uuid']

    showexplorer: false

    constructor: (rs, modals, scope, Mines, Projects, uuid) ->

      @pathToHere = []
      @breadcrumbs = [] # TODO: remove?
      @templates = []
      @mines = Mines.all()
      @status = isOpen: false
      # Change name to focus? youAreHere? currentProject?
      @level = child_nodes: [], contents: []
      @allProjects = []

    getHref: ({source, item_type, item_id}) ->
      src = L.findWhere @mines, name: source
      switch item_type
        when 'List'
          "starting-point/choose-list/#{ src.name }?name=#{ item_id }"

    # This smells terrible - TODO investigate!
    getFormName: (project) -> "folder#{ project.id }"

    emptyMessage: (item) ->
      (not @level.child_nodes?.length) and (not @level.contents?.length)

    # NB: was updatefolder!
    updateProject: (project) ->
      updating = Projects.update project.id, project
      updating.then -> # TODO: update the UI

    checkEmpty: (value) ->
      "Please provide a value" unless value

    # It strikes me that this is not the most efficient way to do this. 
    # Will this suck at 100s of projects?
    getName: (id) ->
      L.findWhere @allProjects, {id}
       .title

    # TODO: this should be an ng-class directive!
    getIcon: ({type, item_type}) -> switch
      when type is 'Project' then 'fa fa-folder'
      when item_type is 'List' then 'fa fa-list'
      when item_type is 'Template' then 'fa fa-search'

    deleteProject: (project) ->
      Projects.delete project.id
              .then => @sync()

    getContentCount: ({contents, child_nodes}) ->
      contents.length + L.sum(@getContentCount sp for sp in child_nodes)

    # All sync really needs to do is fetch the projects.
    # The breadcrumbs should be a path to the data.
    sync: ->
      @allProjects = Projects.all()

    # TODO: make this work.
    createProject: (title) ->
      data = {title, type: 'Project'}
      parent = @level.id
      creating = if parent
        Projects.addItem parent, data
      else
        Projects.create data

      creating.then => @sync()
