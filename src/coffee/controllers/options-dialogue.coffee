define ['imjs'], (imjs) ->
  injectables = ['$scope', '$http', '$modalInstance', 'assign', 'Mines']

  Array injectables..., (scope, http, dialogue, assign, Mines) ->

    defaultService = (s) ->
      s.active = true
      s.oldToken = s.token
      return s

    init = ->
      all = Mines.all()
      all.then(map defaultService).then assign scope, 'services'

    scope.ServiceController = Array '$scope', '$timeout', (scope, to) ->
      scope.$watch 'service.token', (token) ->
        conn = imjs.Service.connect root: scope.service.root, token: token
        conn.whoami().then assign(scope.service, 'user'), (e) -> to -> scope.service.user = null

      scope.saveChanges = ->
        scope.service.editing = false
        Mines.put(scope.service.name, {token: scope.service.token}).then init

    map = (f) -> (xs) -> xs.map(f)

    init()

    scope.ok = -> dialogue.close()

    scope.cancel = -> dialogue.dismiss('cancel')

    scope.resetService = ({name, root}) ->
      http.get(root + "/session").then ({data: {token}}) -> Mines.put(name, {token}).then init

    scope.linkService = (service) ->
      service.editing = true

    scope.cancelEditing = (service) ->
      service.editing = false
      service.token = service.oldToken

