define ['imjs'], (imjs) ->
  injectables = ['$scope', '$modalInstance', 'assign', 'Mines']
  Array injectables..., (scope, dialogue, assign, Mines) ->

    defaultService = (s) ->
      s.active = true
      conn = imjs.Service.connect(s)
      conn.whoami().then assign s, 'user'
      return s

    map = (f) -> (xs) -> xs.map(f)

    all = Mines.all()
    all.then(map defaultService).then assign scope, 'services'

    scope.ok = -> dialogue.close()

    scope.cancel = -> dialogue.dismiss('cancel')

