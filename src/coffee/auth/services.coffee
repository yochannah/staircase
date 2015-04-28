define (require) ->

  ng = require 'angular'
  ga = require 'analytics'

  Services = ng.module 'steps.auth.services', []

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
        loggingIn.then (-> ga 'send', 'event', 'auth', 'login', 'success'), ->
          ga 'send', 'event', 'auth', 'login', 'failure'
          navId.logout()

      onlogout: =>
        loggingOut = @post "/auth/logout"
        loggingOut.then => @options.changeIdentity null
        loggingOut.then (->), -> log.error "Logout failure"

