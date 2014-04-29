require ['angular', 'angular-resource', 'lodash'], (ng, _, L) ->

  asData = ({data}) -> data

  Services = ng.module('steps.services', ['ngResource'])

  Services.value('version', '0.1.0')

  Services.factory 'ClassUtils', Array '$q', '$timeout', (Q, timeout) ->
    setClasses = (scope, grouper, propName = 'outputType') -> (model) -> timeout ->
      ret = Q.defer()
      defaults = scope.defaults or {}
      scope.model = model
      name = (className) -> model.makePath(className).getDisplayName().then (displayName) ->
        {className, displayName}
      group = if not grouper then ((x) -> x) else (names) ->
        names.group = grouper names
        return names

      andThen = (names) -> timeout ->
        scope.classes = L.sortBy names.map(group), ['group', 'displayName']
        defaults[propName] = scope[propName] = switch model.name
          when 'genomic' then L.find scope.classes, {className: 'Gene'}
          when 'testmodel' then L.find scope.classes, {className: 'Employee'}
        ret.resolve scope.classes

      orElse = (e) -> ret.reject e

      Q.all(name cls for cls of model.classes).then andThen, orElse

      return ret.promise

    return {setClasses}

  Services.factory 'authorizer', Array '$http', (http) -> -> http.get('/auth/session').then asData

  Services.factory 'apiTokenPromise', Array 'authorizer', (authorize) -> authorize()

  class WebServiceAuth

    makeAuth = (token) -> Authorization: "Token: #{ token }"

    constructor: (tokenPromise, scope, authorize) ->
      @_p = tokenPromise.then makeAuth
      @headers = {}
      @_p.then (h) => @headers[k] = v for k, v of h
      scope.$watch 'auth.identity', =>
        @_p = authorize().then makeAuth
        @authorize().then (h) => @headers[k] = v for k, v of h

    authorize: -> @_p

  Services.service 'WebServiceAuth', ['apiTokenPromise', '$rootScope', 'authorizer', WebServiceAuth]

  Services.factory 'Mines', Array '$http', '$log', 'WebServiceAuth', (http, logger, auth) ->
    URL = "/api/v1/services"

    all = -> auth.authorize().then (headers) -> http.get(URL, {headers}).then asData

    get = (ident) ->
      auth.authorize().then (headers) -> http.get("#{URL}/#{ident}", {headers}).then asData

    return {all, get}

  Services.factory 'Histories', Array 'WebServiceAuth', '$rootScope', '$http', '$resource', (auth, scope, http, resource) ->
    auth.authorize().then ->
      headers = auth.headers
      resource "/api/v1/histories/:id", {id: '@id'},
        get: {method: 'GET', headers: headers}
        query: {method: 'GET', headers: headers, isArray: true}
        save: {method: 'POST', headers: headers}
        append: {method: 'POST', headers: headers, url: '/api/v1/histories/:id/steps'}

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
