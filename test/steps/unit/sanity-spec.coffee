define ['lodash'], (L) ->

  describe 'Just checking the test harness', ->

    it 'works for lodash', -> expect(L.size [0 .. 9]).toEqual 10

