define ['angularMocks', 'projects/controllers'], (mocks) ->

  describe 'ProjectCtrl', ->

    mockServices = [{name: 'foo'}, {name: 'bar'}]
    mockTemplates = []
    mockLists = []
    mockProjects = -> [ # Function so we get a fresh one every time. Workaround for mutable data.
      {
        type: 'Project'
        id: 1
        title: 'mock project 1'
        item_count: 3
        contents: [{id: 2, item_type: 'List'}, {id: 3, item_type: 'Template'}]
        child_nodes: [
          {
            id: 4
            type: 'Project'
            title: 'mock project 2'
            item_count: 1
            contents: [{id: 5}]
            child_nodes: []
          }
        ]
      }
      {
        id: 6
        type: 'Project'
        title: 'mock project 3'
        item_count: 0
        contents: []
        child_nodes: []
      }
    ]
    mockNewProject = {id: 7, title: 'added', item_count: 0, description: null, contents: [], child_nodes: []}

    # We mock this directly to prevent calls to the InterMine
    # back ends. We can't really intercept them.
    mockGetEntities = -> then: (f) -> f templates: mockTemplates, lists: mockLists

    test = {}

    beforeEach mocks.module 'steps.projects.controllers'

    beforeEach mocks.inject ($rootScope, $injector, $controller) ->
      test.$httpBackend = $injector.get('$httpBackend')
      test.locals =
        $scope: $rootScope.$new()
        $routeParams: {}
        getMineUserEntities: mockGetEntities
        $ngSilentLocation:
          silent: -> # TODO: test calls to this.

      test.$controller = $controller
      test.$httpBackend
          .when 'GET', '/auth/session'
          .respond "mock-token"
      test.$httpBackend
          .when 'GET', '/api/v1/services'
          .respond mockServices
      test.projectHandler = test.$httpBackend.when 'GET', '/api/v1/projects'
      test.projectHandler.respond 200, mockProjects()
      test.$httpBackend
          .when 'POST', '/api/v1/projects/6/items', /"type":\s*"Project"/
          .respond 200, mockNewProject
      test.$httpBackend
          .when 'POST', '/api/v1/projects'
          .respond mockNewProject
      test.$httpBackend
          .when 'POST', '/api/v1/projects/1/items', /"type":\s*"Item"/
          .respond 200, {id: 100}

      test.projects = test.$controller 'ProjectsCtrl', test.locals

    describe 'getHref', ->

      it 'should generate an href for lists', ->
        input =
          source: 'there'
          item_type: 'List'
          item_id: 'that'
        output = "starting-point/choose-list/there?name=that"
        expect(test.projects.getHref input).toEqual output

      it 'should not generate an href for anything else', ->
        input =
          source: 'there'
          item_type: 'Warble'
          item_id: 'foo'
        expect(test.projects.getHref input).not.toBeDefined()

    describe 'checkEmpty', ->

      it 'should provide an error message if the value is null', ->
        expect(test.projects.checkEmpty null).toBe 'Please provide a value'

      it 'should provide an error message if the value is an empty string', ->
        expect(test.projects.checkEmpty '').toBe 'Please provide a value'

      it 'should provide an error message if the value is a blank string', ->
        expect(test.projects.checkEmpty '   ').toBe 'Please provide a value'

      it 'should not complain if there is a value', ->
        expect(test.projects.checkEmpty 'not empty').not.toBeDefined()

    describe 'Initial state', ->

      beforeEach ->
        test.$httpBackend.flush()

      it 'has an empty path to here', ->
        expect(test.projects.pathToHere).toEqual []

      it 'does not want to see the explorer', ->
        expect(test.projects.showExplorer).toBe false

      it 'should always have a currentProject', ->
        expect(test.projects.currentProject).not.toBeNull()

      it 'should have loaded the mines', ->
        expect(test.projects.mines.length).toEqual 2
        expect(m.name for m in test.projects.mines).toEqual ['foo', 'bar']

      it 'should have created a synthetic crrent project', ->
        expect(test.projects.currentProject.child_nodes.length).toEqual 2

      it 'should have two projects', ->
        expect(test.projects.allProjects.length).toEqual 2

      it 'should have the right project data', ->
        expect(test.projects.currentProject.child_nodes[0].id).toEqual 1

      it 'should know that the current project is not empty', ->
        expect(test.projects.currentProject._is_empty).toBe false

    describe 'Initial server load', ->

      afterEach ->
        test.$httpBackend.verifyNoOutstandingExpectation()
        test.$httpBackend.verifyNoOutstandingRequest()

      it 'should have made a bunch of requests to the server', ->
        test.$httpBackend.expectGET '/auth/session'
        test.$httpBackend.expectGET '/api/v1/services',
          Authorization: 'Token: mock-token'
          Accept: "application/json, text/plain, */*"
        test.$httpBackend.flush()

    describe 'Selecting a project', ->

      beforeEach ->
        test.projects.setCurrentProject mockProjects()[1]

      it 'should have added a project to the path', ->
        expect(test.projects.pathToHere.length).toEqual 1

      it 'should have the right names on the path', ->
        expect(s.name for s in test.projects.pathToHere).toEqual ['mock project 3']

      it 'should now have that project as the current project', ->
        expect(test.projects.currentProject.id).toBe 6

      it 'should know that the current project is empty', ->
        expect(test.projects.currentProject._is_empty).toBe true

    describe 'Adding a project', ->

      beforeEach ->
        test.projects.createProject 'added'
        test.projectHandler.respond 200, mockProjects().concat([mockNewProject])

      afterEach ->
        test.$httpBackend.verifyNoOutstandingRequest()

      it 'should have made a call to the back end to create the project', ->
        test.$httpBackend.expectPOST '/api/v1/projects', {type: 'Project', title: 'added'}
        test.$httpBackend.flush()
        test.$httpBackend.verifyNoOutstandingExpectation()

      it 'should have updated the projects list', ->
        test.$httpBackend.flush()
        expect(test.projects.allProjects.length).toBe 3

    describe 'Adding a project to an empty project', ->

      beforeEach ->
        test.$httpBackend.flush()
        test.projects.setCurrentProject mockProjects()[1]
        test.projects.createProject 'added'
        updated = mockProjects()
        updated[1].child_nodes.push(mockNewProject)
        test.projectHandler.respond 200, updated

      it 'should think the project is empty before we add anything to it', ->
        expect(test.projects.currentProject._is_empty).toBe true

      it 'should know that the current project is not empty', ->
        test.$httpBackend.flush()
        expect(test.projects.currentProject._is_empty).toBe false

    describe 'Adding a nested project', ->

      beforeEach ->
        test.projects.setCurrentProject mockProjects()[1]
        test.projects.createProject 'added'
        updated = mockProjects()
        updated[1].child_nodes.push mockNewProject
        test.projectHandler.respond 200, updated

      afterEach ->
        test.$httpBackend.verifyNoOutstandingRequest()

      it 'should have made a call to the back end to create the project', ->
        test.$httpBackend.expectPOST '/api/v1/projects/6/items', {type: 'Project', title: 'added'}
        test.$httpBackend.flush()
        test.$httpBackend.verifyNoOutstandingExpectation()

      it 'should not have changed the number of root projects', ->
        test.$httpBackend.flush()
        expect(test.projects.allProjects.length).toBe 2

      it 'should have updated the current project', ->
        expect(test.projects.currentProject.child_nodes.length).toBe 0
        test.$httpBackend.flush()
        expect(test.projects.currentProject.child_nodes.length).toBe 1

    describe 'addItem', ->

      beforeEach ->
        thing =
          source: 'there'
          type: 'List'
          id: 'added thing'

        updated = mockProjects()
        updated[0].contents.push
          id: 100
          source: 'there'
          item_type: 'List'
          item_id: 'added thing'
        updated[0].item_count++

        test.projectHandler.respond 200, updated

        test.projects.addItem thing, 1

      afterEach ->
        test.$httpBackend.verifyNoOutstandingRequest()

      it 'should have made a call to the back end to add the item', ->
        test.$httpBackend.expectPOST '/api/v1/projects/1/items',
          type: 'Item'
          source: 'there'
          item_type: 'List'
          item_id: 'added thing'
        test.$httpBackend.flush()
        test.$httpBackend.verifyNoOutstandingExpectation()

      it 'should not have caused an error', ->
        expect(test.projects.error).not.toBeDefined()

      it 'should not have changed the number of root projects', ->
        test.$httpBackend.flush()
        expect(test.projects.allProjects.length).toBe 2

      it 'should have updated the target project', ->
        test.$httpBackend.flush()
        target = test.projects.currentProject.child_nodes[0]
        expect(target.contents.length).toBe 3
        expect(target.item_count).toBe 4
        expect(target.contents[2].item_id).toEqual 'added thing'

    describe 'using drag and drop', ->

      beforeEach ->
        thing =
          source: 'there'
          type: 'List'
          id: 'dropped thing'
        dest = mockProjects()[0]

        updated = mockProjects()
        updated[0].contents.push
          id: 100
          source: 'there'
          item_type: 'List'
          item_id: 'dropped thing'
        updated[0].item_count++

        test.$httpBackend.flush()
        test.projectHandler.respond 200, updated

        test.projects.dropped JSON.stringify(thing), JSON.stringify(dest)

      afterEach ->
        test.$httpBackend.verifyNoOutstandingRequest()
        test.$httpBackend.verifyNoOutstandingExpectation()

      it 'should have made a call to the back end to add the item', ->
        test.$httpBackend.expectPOST '/api/v1/projects/1/items',
          type: 'Item'
          source: 'there'
          item_type: 'List'
          item_id: 'dropped thing'
        test.$httpBackend.flush()

      it 'should not have changed the number of root projects', ->
        test.$httpBackend.flush()
        expect(test.projects.allProjects.length).toBe 2

      it 'should have updated the current project', ->
        test.$httpBackend.flush()
        target = test.projects.currentProject.child_nodes[0]
        expect(target.contents.length).toBe 3
        expect(target.item_count).toBe 4

    describe 'updating a project', ->

      beforeEach ->
        test.$httpBackend.flush()
        # The user would provide a value using the editable form...
        target = test.projects.allProjects[0]
        target.description = 'edited'
        test.projects.updateProject target

        test.$httpBackend
            .when 'PUT', '/api/v1/projects/1', /"description":\s*"edited"/
            .respond 200, {id: 1}
        updated = mockProjects()
        updated[0].description = 'edited, and saved'

        test.projectHandler.respond 200, updated

      afterEach ->
        test.$httpBackend.verifyNoOutstandingRequest()
        test.$httpBackend.verifyNoOutstandingExpectation()

      it 'should have made a call to the back end to save the data', ->
        test.$httpBackend.expectPUT '/api/v1/projects/1',
          title: 'mock project 1'
          description: 'edited'

        test.$httpBackend.flush()

      it 'should update the current data', ->
        test.$httpBackend.flush()
        target = test.projects.currentProject.child_nodes[0]
        expect(target.description).toEqual 'edited, and saved'
    
    describe 'deleting a project', ->

      beforeEach ->
        test.$httpBackend.flush()
        test.projects.deleteProject test.projects.allProjects[0]

        test.$httpBackend
            .when 'DELETE', '/api/v1/projects/1'
            .respond 204

        updated = mockProjects().slice 1
        test.projectHandler.respond 200, updated

      afterEach ->
        test.$httpBackend.verifyNoOutstandingRequest()
        test.$httpBackend.verifyNoOutstandingExpectation()

      it 'should make a call to the back end to delete the project', ->
        test.$httpBackend.expectDELETE '/api/v1/projects/1'
        test.$httpBackend.flush()

      it 'should remove the deleted project from the current data', ->
        test.$httpBackend.flush()
        expect(test.projects.currentProject.child_nodes[0].id).toBe 6
        expect(test.projects.allProjects.length).toEqual 1

    describe 'deleting an item', ->

      beforeEach ->
        test.$httpBackend.flush()
        test.projects.setCurrentProject test.projects.allProjects[0]
        test.projects.deleteItem test.projects.currentProject, test.projects.currentProject.contents[0]

        test.$httpBackend
            .when 'DELETE', '/api/v1/projects/1/items/2'
            .respond 204

        updated = mockProjects()
        updated[0].contents.shift() # Pops first
        test.projectHandler.respond 200, updated

      afterEach ->
        test.$httpBackend.verifyNoOutstandingRequest()
        test.$httpBackend.verifyNoOutstandingExpectation()

      it 'should make a call to the back end to delete the item', ->
        test.$httpBackend.expectDELETE '/api/v1/projects/1/items/2'
        test.$httpBackend.flush()

      it 'should have updated the current project', ->
        expect(test.projects.currentProject.contents.length).toEqual 2
        test.$httpBackend.flush()
        expect(test.projects.currentProject.contents.length).toEqual 1

