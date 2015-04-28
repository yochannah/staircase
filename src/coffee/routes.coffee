define [], -> (app) ->

  dependencies = auth: ['WebServiceAuth', (auth) -> auth.authorize()]
  controllerName = 'appView' # Mount controllers as this name.
  routeTable =
    '/':                     ['IndexCtrl',         '/partials/frontpage.html']
    '/history/:id/:idx':     ['HistoryCtrl',       '/partials/history.html']
    '/about':                ['AboutCtrl',         '/partials/about.html']
    '/projects':             ['ProjectsCtrl',      '/partials/projects.html']
    '/projects/:pathToHere*':['ProjectsCtrl',      '/partials/projects.html']
    '/starting-point/:tool': ['StartingPointCtrl', '/partials/starting-point.html']
    '/starting-point/:tool/:service': ['StartingPointCtrl', '/partials/starting-point.html']
    '/starting-point/:tool/:args*': ['StartingPointCtrl', '/partials/starting-point.html']

  # Helper to create a route definition.
  route = (templateUrl, controller) ->
    templateUrl: templateUrl
    controller: controller
    controllerAs: controllerName
    resolve: dependencies

  app.config Array '$locationProvider', '$routeProvider', (locations, router) ->
    locations.html5Mode true

    for pattern, [controller, templateUrl] of routeTable
      router.when pattern, route templateUrl, controller
    router.otherwise
      templateUrl: '/partials/404.html'

