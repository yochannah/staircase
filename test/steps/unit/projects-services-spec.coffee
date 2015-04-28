define ['angularMocks', 'projects/services'], (mocks) ->

  test = {}

  mockLists =
    foo: -> []

  mockTemplates =
    foo: -> {}

  describe 'ProjectServices', ->

    getMockImjs = ->
      Service:
        connect: (def) ->
          fetchLists: -> test.Q.when(mockLists[def.name]())
          fetchTemplates: ->
            test.Q.when(mockTemplates[def.name]())

    beforeEach mocks.module 'steps.projects.services', (imjsProvider) ->
      imjsProvider.setImpl getMockImjs()
      # ng gets very upset if this function returns anything, so
      null # <-- this is really important 

    beforeEach mocks.inject ($rootScope, $q, $injector, Projects, getMineUserEntities) ->
        test.scope = $rootScope
        test.Q = $q
        test.$httpBackend = $injector.get '$httpBackend'

        test.$httpBackend
            .when 'GET', '/auth/session'
            .respond 200, "session"

        test.minesHandler = test.$httpBackend.when 'GET', '/api/v1/services'
        test.minesHandler.respond 200, []

        test.getMineUserEntities = getMineUserEntities
        test.Projects = Projects


    describe 'setup', ->
      it 'there should be a getMineUserEntities service', ->
        expect(test.getMineUserEntities).toBeDefined()

      it 'there should be a Projects service', ->
        expect(test.Projects).toBeDefined()

