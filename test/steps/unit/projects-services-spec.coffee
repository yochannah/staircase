define ['angularMocks', 'projects/services'], (mocks) ->

  getMockImjs = -> {}

  describe 'ProjectServices', ->

    test = {}

    beforeEach mocks.module 'steps.projects.services', (imjsProvider) ->
      imjsProvider.setImpl getMockImjs()
      null

    beforeEach mocks.inject ($rootScope, $injector, getMineUserEntities) ->
        test.scope = $rootScope
        test.$httpBackend = $injector.get '$httpBackend'

        test.$httpBackend
            .when 'GET', '/auth/session'
            .respond 200, "session"

        test.minesHandler = test.$httpBackend
                                .when 'GET', '/api/v1/services'
                                .respond 200, []

        test.getMineUserEntities = getMineUserEntities

    describe 'setup', ->
      it 'there should be a service', ->
        expect(test.getMineUserEntities).toBeDefined()

      it 'should return a value', ->
        expect(test.getMineUserEntities()).toBeDefined()

      it 'should have a then function', ->
        expect(test.getMineUserEntities().then).toBeDefined()

    describe 'with 0 configured mines', ->

      afterEach ->
         test.$httpBackend.verifyNoOutstandingExpectation()
         test.$httpBackend.verifyNoOutstandingRequest()

      it 'should at least request the list of services', ->
        test.$httpBackend.expectGET '/api/v1/services'
        test.getMineUserEntities()
        test.$httpBackend.flush()

      it 'should return a valid but empty result', ->
        success = jasmine.createSpy 'success'
        error = jasmine.createSpy 'error'
        test.getMineUserEntities().then success, error
        test.$httpBackend.flush() # return http result
        expect(success).toHaveBeenCalledWith {lists: [], templates: []}
        expect(error).not.toHaveBeenCalled()

