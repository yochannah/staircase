define (require) ->

  ng = require 'angular'
  require './services'

  Controllers = ng.module 'steps.auth.controllers', ['steps.auth.services']

  Controllers.controller 'AuthController', class AuthController

    @$inject = ['$rootScope', '$scope', 'Persona']

    constructor: (rs, scope, Persona) ->
      rs.auth ?= identity: null

      changeIdentity = (identity) ->
        scope.auth.identity = identity
        scope.auth.loggedIn = identity?

      scope.persona = new Persona {changeIdentity, loggedInUser: scope.auth.identity}

