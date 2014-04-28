define [], -> (app) ->

  app.config Array '$locationProvider', '$routeProvider', (locations, router) ->
    locations.html5Mode true
    router.when '/',
      templateUrl: '/partials/frontpage.html'
      controller: 'IndexCtrl'
    router.when '/about',
      templateUrl: '/partials/about.html'
      controller: 'AboutCtrl'
