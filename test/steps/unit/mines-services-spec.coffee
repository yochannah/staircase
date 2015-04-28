define ['angularMocks', 'mines/services'], (mocks) ->

  test = {}

  mockLists =
    foo: -> []
    bar: -> [{name: "bar list 1", size: 100}, {name: "bar list 2", size: 101}]
    baz: -> [{name: 'baz list 1', size: 200}]
    failed: -> test.Q.reject 'Could not get lists'

  mockTemplates =
    foo: -> {}
    bar: ->
      bar_template_1: {name: 'bar template 1', prop: 'x'}
      bar_template_2: {name: 'bar template 2', prop: 'y'}
    baz: ->
      baz_template_1: {name: 'baz template 1', prop: 'w'}
    failed: -> test.Q.reject 'Could not get templates'

  describe 'MineServices', ->

    getMockImjs = ->
      Service:
        connect: (def) ->
          fetchLists: -> test.Q.when(mockLists[def.name]())
          fetchTemplates: ->
            test.Q.when(mockTemplates[def.name]())

    beforeEach mocks.module 'steps.mines.services', (imjsProvider) ->
      imjsProvider.setImpl getMockImjs()
      # ng gets very upset if this function returns anything, so
      null # <-- this is really important 

    beforeEach mocks.inject ($rootScope, $q, $injector, getMineUserEntities) ->
        test.scope = $rootScope
        test.Q = $q
        test.$httpBackend = $injector.get '$httpBackend'

        test.$httpBackend
            .when 'GET', '/auth/session'
            .respond 200, "session"

        test.minesHandler = test.$httpBackend.when 'GET', '/api/v1/services'
        test.minesHandler.respond 200, []

        test.getMineUserEntities = getMineUserEntities

    describe 'setup', ->
      it 'there should be a service', ->
        expect(test.getMineUserEntities).toBeDefined()

      it 'should return a value', ->
        expect(test.getMineUserEntities()).toBeDefined()

      it 'should have a then function', ->
        expect(test.getMineUserEntities().then).toBeDefined()

      it 'should have a minesHandler', ->
        expect(test.minesHandler).toBeDefined()

    emptyButValid =
      errors: []
      lists: []
      templates: []

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
        expect(success).toHaveBeenCalledWith emptyButValid
        expect(error).not.toHaveBeenCalled()

    describe 'with 1 configured service that has no entities', ->

      beforeEach ->
        test.minesHandler.respond [{name: 'foo'}]

      it 'should return a valid but empty result', ->
        success = jasmine.createSpy 'success'
        error = jasmine.createSpy 'error'
        test.getMineUserEntities().then success, error
        test.$httpBackend.flush() # return http result
        expect(success).toHaveBeenCalledWith emptyButValid
        expect(error).not.toHaveBeenCalled()

    describe 'with 1 configured service that has some entities', ->

      barsEntities =
        errors : []
        lists: [
          {
            type: 'List'
            id: 'bar list 1'
            source: 'bar'
            meta: undefined
            entity:
              name: 'bar list 1'
              size: 100
          },
          {
            type: 'List'
            id: 'bar list 2'
            source : 'bar'
            meta: undefined
            entity:
              name: 'bar list 2'
              size: 101
          }
        ]
        templates: [
          {
            type: 'Template'
            id: 'bar template 1'
            source: 'bar'
            meta: undefined
            entity:
              name: 'bar template 1'
              prop: 'x'
          },
          {
            type: 'Template'
            id: 'bar template 2'
            source : 'bar'
            meta: undefined
            entity:
              name: 'bar template 2'
              prop: 'y'
          }
        ]

      beforeEach ->
        test.minesHandler.respond [{name: 'bar'}]

      it 'should return results from bar', ->
        success = jasmine.createSpy 'success'
        error = jasmine.createSpy 'error'
        test.getMineUserEntities().then success, error
        test.$httpBackend.flush() # return http result
        expect(success).toHaveBeenCalledWith barsEntities
        expect(error).not.toHaveBeenCalled()

    describe 'with a failed service', ->

      failedEntities =
        errors : ['Could not get lists', 'Could not get templates']
        lists: []
        templates: []

      beforeEach ->
        test.minesHandler.respond [{name: 'failed'}]

      it 'should return results from bar', ->
        success = jasmine.createSpy 'success'
        error = jasmine.createSpy 'error'
        test.getMineUserEntities().then success, error
        test.$httpBackend.flush() # return http result
        expect(success).toHaveBeenCalledWith failedEntities
        expect(error).not.toHaveBeenCalled()

    describe 'with all configured services', ->

      allEntities =
        errors : ['Could not get lists', 'Could not get templates']
        lists: [
          {
            type: 'List'
            id: 'bar list 1'
            source: 'bar'
            meta: undefined
            entity:
              name: 'bar list 1'
              size: 100
          },
          {
            type: 'List'
            id: 'bar list 2'
            source : 'bar'
            meta: undefined
            entity:
              name: 'bar list 2'
              size: 101
          },
          {
            type: 'List'
            id: 'baz list 1'
            source : 'baz'
            meta: undefined
            entity:
              name: 'baz list 1'
              size: 200
          }
        ]
        templates: [
          {
            type: 'Template'
            id: 'bar template 1'
            source: 'bar'
            meta: undefined
            entity:
              name: 'bar template 1'
              prop: 'x'
          },
          {
            type: 'Template'
            id: 'bar template 2'
            source : 'bar'
            meta: undefined
            entity:
              name: 'bar template 2'
              prop: 'y'
          },
          {
            type: 'Template'
            id: 'baz template 1'
            source: 'baz'
            meta: undefined
            entity:
              name: 'baz template 1'
              prop: 'w'
          },
        ]

      beforeEach ->
        test.minesHandler.respond [
          {name: 'foo'}, {name: 'bar'}, {name: 'baz'}, {name: 'failed'}
        ]

      it 'should return everthing it can find', ->
        success = jasmine.createSpy 'success'
        error = jasmine.createSpy 'error'
        test.getMineUserEntities().then success, error
        test.$httpBackend.flush() # return http result
        expect(success).toHaveBeenCalledWith allEntities
        expect(error).not.toHaveBeenCalled()

    describe 'entities from services with metadata', ->

      entitiesWithMetadata =
        errors : []
        lists: [
          {
            type: 'List'
            id: 'baz list 1'
            source : 'baz'
            meta: 'meta of baz'
            entity:
              name: 'baz list 1'
              size: 200
          }
        ]
        templates: [
          {
            type: 'Template'
            id: 'baz template 1'
            source: 'baz'
            meta: 'meta of baz'
            entity:
              name: 'baz template 1'
              prop: 'w'
          },
        ]

      beforeEach ->
        test.minesHandler.respond 200, [{name: 'baz', meta: 'meta of baz'}]

      it 'should make sure all the entities have the correct meta', ->
        success = jasmine.createSpy 'success'
        error = jasmine.createSpy 'error'
        test.getMineUserEntities().then success, error
        test.$httpBackend.flush() # return http result
        expect(success).toHaveBeenCalledWith entitiesWithMetadata
        expect(error).not.toHaveBeenCalled()
