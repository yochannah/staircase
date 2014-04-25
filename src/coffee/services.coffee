require ['angular', 'angular-resource'], (ng) ->

  Services = ng.module('steps.services', ['ngResource'])

  Services.value('version', '0.1.0')

  Services.factory 'Mines', Array '$http', '$log', '$rootScope', (http, logger, scope) ->
    headers = {}

    scope.$watch 'auth.identity', -> # on login/logout
      http.get('/auth/session').then ({data}) -> headers.Authorization = "Token: #{ data }"

    all = ->
      http.get('/api/v1/services', {headers}).then ({data}) -> data

    get = (ident) ->
      http.get('/api/v1/services/' + ident, {headers}).then ({data}) -> data

    return {all, get}

  Services.factory 'Histories', Array '$rootScope', '$http', '$resource', (scope, http, resource) ->
    headers = {}

    scope.$watch 'auth.identity', -> # on login/logout
      http.get('/auth/session').then ({data}) -> headers.Authorization = "Token: #{ data }"

    return resource "/api/v1/histories/:id", {id: '@id'},
      get: {method: 'GET', headers: headers}
      query: {method: 'GET', headers: headers, isArray: true}
      save: {method: 'POST', headers: headers}

  Services.factory 'Persona', Array '$window', '$log', '$http', (win, log, http) ->
    watch = request = logout = -> log.warn "Persona authentication not available."
    navId = win.navigator?.id ? {watch, request, logout}

    class Persona

      constructor: (@options) ->
        navId.watch {loggedInUser: @options.loggedInUser, @onlogin, @onlogout}

      post: (url, data = {}) ->
        csrfP = http.get("/auth/csrf-token").then ({data}) -> data
        csrfP.then (token) ->
          data["__anti-forgery-token"] = token
          http.post url, data

      request: -> navId.request()

      logout: -> navId.logout()

      onlogin: (assertion) =>
        loggingIn = @post "/auth/login", {assertion}
        loggingIn.then ({data}) => @options.changeIdentity data.current
        loggingIn.then (->), -> navId.logout()

      onlogout: =>
        loggingOut = @post "/auth/logout"
        loggingOut.then => @options.changeIdentity null
        loggingOut.then (->), -> log.error "Logout failure"
