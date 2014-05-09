require ['angular', 'angular-resource', 'lodash'], (ng, _, L) ->

  asData = ({data}) -> data

  Services = ng.module('steps.services', ['ngResource'])

  Services.value('version', '0.1.0')

  class StepConfig

    constructor: ->
      stepConfig = {}

      @configureStep = (ident, conf) -> stepConfig[ident] = conf

      @$get = -> stepConfig

  Services.provider 'stepConfig', StepConfig

  # Little pull parsing state machine for tokenising identifiers.
  Services.factory 'tokenise', -> (string) ->
    charIndex = 0
    escaping = false
    inQuotes = false
    current = []
    tokens = []

    endCurrent = ->
      if current.length
        tokens.push current.join ''
        current = []

    isDelimiter = (c) -> /\s/.test(char) or ',' is char

    if string
      while char = string.charAt(charIndex++)
        if inQuotes and !escaping and char is "\\"
          escaping = true
        else if inQuotes and escaping and char is '"'
          current.push(char)
          escaping = false
        else if inQuotes and escaping and char is 'n'
          current.push("\n")
          escaping = false
        else if inQuotes and escaping
          current.push("\\")
          current.push(char)
          escaping = false
        else if inQuotes and !escaping and char is '"'
          inQuotes = false
          endCurrent()
        else if inQuotes
          current.push(char)
        else if !inQuotes and !escaping and char is '"'
          inQuotes = true
        else if !inQuotes and !escaping and isDelimiter(char)
          endCurrent()
        else
          current.push(char)
        
      endCurrent()

    return tokens

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
    headers = auth.headers
    resource "/api/v1/histories/:id", {id: '@id'},
      get: {method: 'GET', headers: headers}
      getStep: {method: 'GET', headers: headers, url: '/api/v1/histories/:id/steps/:idx'}
      getSteps: {method: 'GET', headers: headers, isArray: true, url: '/api/v1/histories/:id/steps'}
      query: {method: 'GET', headers: headers, isArray: true}
      save: {method: 'PUT', headers: headers}
      create: {method: 'POST', headers: headers}
      delete: {method: 'DELETE', headers: headers}
      append: {method: 'POST', headers: headers, url: '/api/v1/histories/:id/steps'}

  Services.factory 'startHistory', Array 'Histories', '$location', (Histories, location) -> (scope) ->
    scope.$on 'start-history', (evt, {thing, verb, tool, data}) ->
      historyTitle = "Started by #{ verb.ing } #{ thing }"
      history = Histories.create {title: historyTitle}, ->
        stepTitle = "#{ verb.ed } #{ thing }"
        step = Histories.append {id: history.id}, {title: stepTitle, tool, data}, ->
          console.log("Created history " + history.id + " and step " + step.id)
          location.url "/history/#{ history.id }/1"

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
