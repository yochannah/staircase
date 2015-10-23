(function() {
  define(function(require) {
    var AuthController, Controllers, ng;
    ng = require('angular');
    require('./services');
    Controllers = ng.module('steps.auth.controllers', ['steps.auth.services']);
    return Controllers.controller('AuthController', AuthController = (function() {
      AuthController.$inject = ['$rootScope', '$scope', 'Persona'];

      function AuthController(rs, scope, Persona) {
        var changeIdentity;
        if (rs.auth == null) {
          rs.auth = {
            identity: null
          };
        }
        changeIdentity = function(identity) {
          scope.auth.identity = identity;
          return scope.auth.loggedIn = identity != null;
        };
        scope.persona = new Persona({
          changeIdentity: changeIdentity,
          loggedInUser: scope.auth.identity
        });
      }

      return AuthController;

    })());
  });

}).call(this);
