define ['angular', 'angularMocks', 'services', 'should'], (ng, mocks) ->

  describe 'service', ->

    beforeEach mocks.module 'steps.services'

    describe 'version', ->
      it 'should return the current version', mocks.inject (version) ->
        version.should.equal '0.1.0'

