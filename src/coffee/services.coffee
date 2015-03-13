# Need to capture this reference to the requirejs require function so
# it is available to load next steps with, since it is different
# from the require function injected into the commonjs wrapper.
requirejs = require

define (require, exports, module) ->

  ng = require 'angular'
  require 'angular-resource'
  L = require 'lodash'
  imjs = require 'imjs'
  Messages = require './messages'
  ga = require 'analytics'

  asData = ({data}) -> data

  invoke = (methodName, args...) -> (callee) -> callee[methodName](args...)

  Services = ng.module('steps.services', ['ngResource'])

  Services.value('version', '0.1.0')

  Services.factory 'assign', ['$timeout', (to) -> (obj, prop) -> (x) -> to -> obj[prop] = x]

  Services.factory 'Messages', -> new Messages

  class StepConfig

    constructor: ->

      stepConfig = {}

      @configureStep = (ident, conf) -> stepConfig[ident] = conf

      @$get = -> stepConfig

  Services.provider 'stepConfig', StepConfig

  # A service for getting a computed style.
  Services.factory 'getComputedStyle', ($window) ->
    body = $window.document.getElementsByTagName('body')[0]
    e = $window.document.createElement 'div'
    addDummy = (className) ->
      e = $window.document.createElement('div')
      e.className = className
      e.style.display = 'none'
      body.appendChild(e)
      return e

    if e.currentStyle
      return (className, prop) ->
        elem = addDummy className
        style = elem.currentStyle[prop]
        body.removeChild elem
        return style
    else if $window.getComputedStyle
      return (className, prop) ->
        elem = addDummy className
        style = $window.document.defaultView.getComputedStyle(elem, null)[prop]
        body.removeChild elem
        return style
    else
      return -> null

  Services.factory 'meetRequest', Array '$q', '$injector', (Q, injector) -> (tool, step, data) ->
    d = Q.defer()
    requireRelativeToBase = requirejs.config baseUrl: '/'
    requireRelativeToBase ['.' + tool.providerURI], (factory) ->
      handleRequest = injector.invoke factory, this
      handleRequest(step, data).then d.resolve, d.reject
      
    return d.promise

  do (deps = ['$cacheFactory', 'localStorageService']) ->
    Services.factory 'localCache', Array deps..., (cacheFactory, localStorage) ->

      restoreCache = (cache, name) ->
        for k in localStorage.keys() when 0 is k.indexOf "#{ name }:"
          cache.put k.slice(name.length + 1), localStorage.get(k)

      constructCache = (name) ->
        cache = cacheFactory name, capacity: 1000
        restoreCache cache, name
        origPut = cache.put
        newPut = (k, v) ->
          localStorage.set "#{name}:#{ k }", JSON.stringify(v)
          origPut.call cache, k, v
        cache.put = newPut
        return cache

      (name) ->
        if cache = cacheFactory.get name
          return cache
        else
          cache = constructCache name

  Services.factory 'generateListName', Array '$q', (Q) -> (conn, type, category) ->
    naming = conn.fetchModel().then (model) -> model.makePath(type).getDisplayName()
    gettingLists = conn.fetchLists()
    Q.all([naming, gettingLists]).then ([name, lists]) ->
      currentNames = L.pluck lists, 'name'
      baseName = "#{ name } list - #{ new Date().toDateString() }"
      baseName = "#{ category } #{ baseName }" if category
      currentName = baseName
      suffix = 1
      while L.contains currentNames, currentName
        currentName = "#{ baseName } (#{ suffix++ })"
      return currentName

  createConnection = (conf) ->
    s = imjs.Service.connect conf
    s.name = conf.name
    return s

  # Provide a function for connecting to a mine by URL.
  Services.factory 'connectTo', Array 'Mines', (mines) -> (root) ->
    mines.atURL(root).then createConnection

  # Connect to a mine by name
  Services.factory 'connect', Array 'Mines', (Mines) ->
    cache = {}
    (name) -> cache[name] ?= Mines.get(name).then createConnection

  # Provide a function that will yield a value suitable for identifying
  # which version of a service was contacted at a particular time.
  Services.factory 'serviceStamp', Array '$q', 'connectTo', (Q, connectTo) -> (service) ->
    if service?.root
      getService = connectTo service.root
      getVersion = getService.then invoke 'fetchVersion'
      getRelease = getService.then invoke 'fetchRelease'
      Q.all([getService, getVersion, getRelease]).then ([{root}, version, release]) ->
        "#{ root }@#{ version }-#{ release }"
    else
      Q.when null

  Services.factory 'getIdQuery', Array '$q', (Q) -> (type, ids) -> (service) ->
    getModel = service.fetchModel()
    getCKs = service.fetchClassKeys()
    getQuery = Q.all([getCKs, getModel]).then ([classkeys, model]) ->
      keys = (classkeys[type] or ("#{ item.type }.#{ a }" for a of model.classes[type].attributes when a isnt 'id'))
      query = select: keys, where: [{path: type, op: 'IN', ids: ids}]

  # Provide a function of the form:
  # :: (ConnectionArgs, {type :: string, id :: integer}) -> Promise<Object<string, string>>
  do (name = 'identifyItem', deps = ['$q', 'connectTo', 'getIdQuery']) ->
    Services.factory name, Array deps..., (Q, connectTo, getIdQuery) -> (service, item) ->
      getService = connectTo service.root
      getQuery = getService.then getIdQuery item.type, [item.id]

      Q.all([getService, getQuery]).then ([service, query]) ->
        service.rows(query).then ([row]) ->
          fields = L.zipObject query.select, row
          L.mapValues(L.omit(fields, (value) -> value == null), String)

  # Provide a function of the form:
  # :: (ConnectionArgs, {type :: string, ids :: [integer]}) -> Promise<Object<string, [string]>>
  do (name = 'identifyItems', deps = ['$q', 'connectTo']) ->
    Services.factory name, Array deps..., (Q, connectTo) -> (service, items) ->
      getService = connectTo service.root
      getQuery = getService.then getIdQuery items.type, items.ids

      Q.all([getService, getQuery]).then ([service, query]) ->
        service.rows(query).then (rows) ->
          fields = L.zipObject query.select, query.select.map -> []
          for row in rows
            for value, i in row when cell?
              fields[query.select[i]].push String(value)

          L.mapValues fields, (values) -> L.uniq(values)

  Services.factory 'makeList', Array '$q', 'connectTo', 'generateListName', (Q, connectTo, genName) ->
    maker = {}
    
    maker.fromIds = ({listDetails, objectIds, type, service}, category) -> connectTo(service.root).then (conn) ->
      constraint = {path: type, op: 'IN', ids: objectIds}

      naming = (listDetails?.name ? (genName conn, type, category))
      tags = ['source:identifiers'].concat(listDetails?.tags ? [])
      description = (listDetails?.description ? "Created by resolving identifiers")

      querying = conn.query select: ['id'], from: type, where: [constraint]
      Q.all([naming, querying]).then ([name, query]) ->
        # TODO: add tags when we can deal with anonymous user issues.
        query.saveAsList {name, description} # , tags}

    maker.fromQuery = (query, service, listDetails) -> connectTo(service.root).then (conn) ->
      description = listDetails?.description
      console.log query
      querying = conn.query query
      naming = (listDetails?.name ? (querying.then (q) -> genName conn, q.root))
      Q.all([naming, querying]).then ([name, query]) -> query.saveAsList {name, description}

    return maker

  # Little pull parsing state machine for tokenising identifiers.
  Services.factory 'tokenise', -> (string, delimiters) ->
    delimiters ?= [' ', ',', "\n", "\t", ';']
    charIndex = 0
    escaping = false
    inQuotes = false
    current = []
    tokens = []

    endCurrent = ->
      if current.length
        tokens.push current.join ''
        current = []

    isDelimiter = (c) -> c in delimiters

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
      name = (className) ->
        trimmed = className.replace /^.*\./, ''
        model.classes[trimmed] = model.classes[className] # deal with java.lang.Object
        model.makePath(trimmed).getDisplayName()
             .then (displayName) -> {className, displayName}
      group = if not grouper then ((x) -> x) else (names) ->
        names.group = grouper names
        return names

      setProp = (path) -> (obj) -> (val) ->
        [segs..., prop] = path.split('.')
        target = segs.reduce ((o, seg) -> o[seg] or (o[seg] = {})), obj
        target[prop] = val

      andThen = (names) -> timeout ->
        scope.classes = L.sortBy names.map(group), ['group', 'displayName']
        set = setProp propName
        def = switch model.name
          when 'genomic' then L.find scope.classes, {className: 'Gene'}
          when 'testmodel' then L.find scope.classes, {className: 'Employee'}

        set(defaults)(def)
        set(scope)(def)
        ret.resolve scope.classes

      orElse = (e) -> ret.reject e

      Q.all(name cls for cls of model.classes).then andThen, orElse

      return ret.promise

    # (string) -> (Model) -> [{name :: string, path :: Path}]
    getSubTypesOf = (topType) -> (model) -> # All the Sequence Features, with their names.
      classes = (model.makePath(c) for c in model.getSubclassesOf(topType))
      namings = for cp in L.sortBy(classes, String) then do (cp) ->
        cp.getDisplayName().then (name) -> {name: name, path: cp}

      Q.all namings

    return {setClasses, getSubTypesOf}

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

    atURL = (url) -> all().then (mines) ->
      conf = L.find mines, (m) -> (m.root.indexOf(url) >= 0) || (url && url.indexOf(m.root) >= 0)
      if conf?
        return conf
      else
        throw new Error("No mine configured at #{ url }")

    get = (ident) ->
      auth.authorize().then (headers) -> http.get("#{URL}/#{ident}", {headers}).then asData

    put = (ident, data) ->
      auth.authorize().then (headers) -> http.put("#{URL}/#{ident}", data, {headers}).then asData

    return {all, get, atURL, put}

  Services.factory 'Histories', Array 'WebServiceAuth', '$rootScope', '$http', '$resource', (auth, scope, http, resource) ->
    headers = auth.headers
    resource "/api/v1/histories/:id", {id: '@id'},
      get: {method: 'GET', headers: headers}
      getStep: {method: 'GET', headers: headers, url: '/api/v1/histories/:id/steps/:idx'}
      getSteps: {method: 'GET', headers: headers, isArray: true, url: '/api/v1/histories/:id/steps'}
      query: {method: 'GET', headers: headers, isArray: true}
      save: {method: 'PUT', headers: headers}
      fork: {method: 'POST', headers: headers, params: {at: '@at'}, url: '/api/v1/histories/:id/steps/:at/fork'}
      create: {method: 'POST', headers: headers}
      delete: {method: 'DELETE', headers: headers}
      append: {method: 'POST', headers: headers, url: '/api/v1/histories/:id/steps'}

  do (name = 'historyListener', deps = ['Histories', '$log', '$location', 'serviceStamp']) ->
    Services.factory name, Array deps..., (H, console, loc, stamp) ->

      startHistory = ({thing, verb, tool, data}) ->
        historyTitle = "Started by #{ verb.ing } #{ thing }"
        history = H.create {title: historyTitle}, ->
          stepTitle = "#{ verb.ed } #{ thing }"
          stamp(data?.service).then (stamp) ->
            console.debug "Stamp: #{ stamp }", data
            step = H.append {id: history.id}, {title: stepTitle, tool, data, stamp}, ->
              console.debug "Created history #{ history.id } and step #{ step.id }"
              loc.url "/history/#{ history.id }/1"

      watch = (scope) -> scope.$on 'start-history', (evt, message) ->
        startHistory message
        ga 'send', 'event', 'history', 'start', message.tool

      return {watch, startHistory}

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
        loggingOut.then (-> ga 'send', 'event', 'auth', 'logout', 'success'), ->
          ga 'send', 'event', 'auth', 'logout', 'failure'
          log.error "Logout failure"
