(function() {
  var __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; };

  define(function(require) {
    var Services, ga, ng;
    ng = require('angular');
    ga = require('analytics');
    Services = ng.module('steps.auth.services', []);
    return Services.factory('Persona', Array('$window', '$log', '$http', function(win, log, http) {
      var Persona, logout, navId, request, watch, _ref, _ref1;
      watch = request = logout = function() {
        return log.warn("Persona authentication not available.");
      };
      navId = (_ref = (_ref1 = win.navigator) != null ? _ref1.id : void 0) != null ? _ref : {
        watch: watch,
        request: request,
        logout: logout
      };
      return Persona = (function() {
        function Persona(options) {
          this.options = options;
          this.onlogout = __bind(this.onlogout, this);
          this.onlogin = __bind(this.onlogin, this);
          navId.watch({
            loggedInUser: this.options.loggedInUser,
            onlogin: this.onlogin,
            onlogout: this.onlogout
          });
        }

        Persona.prototype.post = function(url, data) {
          var csrfP;
          if (data == null) {
            data = {};
          }
          csrfP = http.get("/auth/csrf-token").then(function(_arg) {
            var data;
            data = _arg.data;
            return data;
          });
          return csrfP.then(function(token) {
            data["__anti-forgery-token"] = token;
            return http.post(url, data);
          });
        };

        Persona.prototype.request = function() {
          return navId.request();
        };

        Persona.prototype.logout = function() {
          return navId.logout();
        };

        Persona.prototype.onlogin = function(assertion) {
          var loggingIn;
          loggingIn = this.post("/auth/login", {
            assertion: assertion
          });
          loggingIn.then((function(_this) {
            return function(_arg) {
              var data;
              data = _arg.data;
              return _this.options.changeIdentity(data.current);
            };
          })(this));
          return loggingIn.then((function() {
            return ga('send', 'event', 'auth', 'login', 'success');
          }), function() {
            ga('send', 'event', 'auth', 'login', 'failure');
            return navId.logout();
          });
        };

        Persona.prototype.onlogout = function() {
          var loggingOut;
          loggingOut = this.post("/auth/logout");
          loggingOut.then((function(_this) {
            return function() {
              return _this.options.changeIdentity(null);
            };
          })(this));
          return loggingOut.then((function() {}), function() {
            return log.error("Logout failure");
          });
        };

        return Persona;

      })();
    }));
  });

}).call(this);
