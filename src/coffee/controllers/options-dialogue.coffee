define ['imjs'], (imjs) ->
  injectables = ['$scope', '$http', '$timeout', '$modalInstance', 'assign', 'Mines']

  Array injectables..., (scope, http, to, dialogue, assign, Mines) ->

    defaultService = (s) ->
      s.active = true
      s.oldToken = s.token
      return s

    init = ->
      scope.adding = {active: false, mine: {scheme: 'http'}}
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

    scope.$watch 'adding.mine.token', (token) ->
      return unless scope.adding.mine.root and token
      conn = imjs.Service.connect scope.adding.mine
      setError = (val) -> () -> to -> scope.adding.authError = val
      conn.whoami().then setError(false), setError(true)

    scope.$watch 'adding.mine.root', (root) ->
      return unless root
      setError = (val) -> () -> to -> scope.adding.urlError = val
      url = "#{ scope.adding.mine.scheme }://#{ root }/version"
      http.get(url).then setError(false), setError(true)

    scope.addService = ->
      {name, root, scheme, token} = scope.adding.mine
      return unless name and root
      to -> scope.adding.inProgress = true
      success = -> to ->
        scope.adding.active = false
        init()
      error = (e) -> to ->
        scope.adding.inProgress = false
        scope.adding.error = e
      Mines.put(name, {name, token, root: "#{ scheme }://#{ root }"}).then success, error

    scope.resetService = ({name, root}) ->
      http.get(root + "/session").then ({data: {token}}) -> Mines.put(name, {token}).then init

    scope.deleteService = ({name}) ->
      Mines.delete(name).then init

    scope.linkService = (service) ->
      service.editing = true

    scope.cancelEditing = (service) ->
      service.editing = false
      service.token = service.oldToken

