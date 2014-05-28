define ['imjs'], (imjs) ->
  injectables = ['$scope', '$http', '$modalInstance', 'assign', 'Mines']
  Array injectables..., (scope, http, dialogue, assign, Mines) ->

    defaultService = (s) ->
      s.active = true
      conn = imjs.Service.connect(s)
      conn.whoami().then assign s, 'user'
      return s

    init = ->
      all = Mines.all()
      all.then(map defaultService).then assign scope, 'services'

    map = (f) -> (xs) -> xs.map(f)

    init()

    scope.ok = -> dialogue.close()

    scope.cancel = -> dialogue.dismiss('cancel')

    scope.resetService = ({name, root}) ->
      http.get(root + "/session").then ({data: {token}}) -> Mines.put(name, {token}).then init

