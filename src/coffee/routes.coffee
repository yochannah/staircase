define ['angular-route', 'app'], (ng, app) ->

  app.config Array '$routeProvider', (router) ->
    router.when '/view1',
      templateUrl: '/partials/partial1.html'
      controller: 'Inline'
    router.when '/view2',
      templateUrl: '/partials/partial2.html'
      controller: 'Required'
