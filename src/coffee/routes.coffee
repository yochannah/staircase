define [], -> (app) ->

  app.config Array '$locationProvider', '$routeProvider', (locations, router) ->
    locations.html5Mode true
    dependencies = auth: ['WebServiceAuth', (auth) -> auth.authorize()]
    router.when '/',
      templateUrl: '/partials/frontpage.html'
      controller: 'IndexCtrl'
      resolve: dependencies
    router.when '/history/:id/:idx',
      templateUrl: '/partials/history.html'
      controller: 'HistoryCtrl'
      resolve: dependencies
    router.when '/starting-point/:tool',
      templateUrl: '/partials/starting-point.html'
      controller: 'StartingPointCtrl'
      resolve: dependencies
    router.when '/starting-point/:tool/:service',
      templateUrl: '/partials/starting-point.html'
      controller: 'StartingPointCtrl'
      resolve: dependencies
    router.when '/aboutnew',
      templateUrl: '/partials/about.html'
      controller: 'AboutCtrl'
    router.when '/projects',
      templateUrl: '/partials/projects.html'
      controller: 'ProjectsCtrl'