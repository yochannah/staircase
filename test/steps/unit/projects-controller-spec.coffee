define ['angularMocks', 'projects/controllers'], (mocks) ->

  describe 'ProjectCtrl', ->

    mockServices = [{name: 'foo'}, {name: 'bar'}]
    mockTemplates = []
    mockLists = []
    mockProjects = [
      {
        id: 1
        title: 'mock project 1'
        item_count: 3
        contents: [{id: 2}, {id: 3}]
        child_nodes: [
          {
            id: 4
            title: 'mock project 2'
            item_count: 1
            contents: [{id: 5}]
            child_nodes: []
          }
        ]
      }
      {
        id: 6
        title: 'mock project 3'
        item_count: 0
        contents: []
        child_nodes: []
      }
    ]

    # We mock this directly to prevent calls to the InterMine
    # back ends. We can't really intercept them.
    mockGetEntities = -> then: (f) ->
      f templates: mockTemplates, lists: mockLists

    test = {}

    beforeEach mocks.module 'steps.projects.controllers'

    beforeEach  mocks.inject ($rootScope, $injector, $controller) ->
      test.scope = $rootScope.$new()
      test.$httpBackend = $injector.get('$httpBackend')
      test.$controller = $controller
      test.$httpBackend
          .when 'GET', '/auth/session'
          .respond "mock-token"
      test.$httpBackend
          .when 'GET', '/api/v1/services'
          .respond mockServices
      test.$httpBackend
          .when 'GET', '/api/v1/projects'
          .respond mockProjects

    describe 'Initial state', ->

      beforeEach -> 
        test.projects = test.$controller 'ProjectsCtrl',
          scope: test.scope
          getMineUserEntities: mockGetEntities
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

    describe 'Initial server load', ->

      beforeEach -> 
        test.projects = test.$controller 'ProjectsCtrl',
          getMineUserEntities: mockGetEntities

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
        test.projects = test.$controller 'ProjectsCtrl',
          getMineUserEntities: mockGetEntities
        test.$httpBackend.flush()
        test.projects.setCurrentProject mockProjects[1]

      it 'should have added a project to the path', ->
        expect(test.projects.pathToHere.length).toEqual 1

      it 'should have the right names on the path', ->
        expect(s.name for s in test.projects.pathToHere).toEqual ['mock project 3']

      it 'should now have that project as the current project', ->
        expect(test.projects.currentProject.id).toBe 6
      
