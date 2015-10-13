(function() {
  var __indexOf = [].indexOf || function(item) { for (var i = 0, l = this.length; i < l; i++) { if (i in this && this[i] === item) return i; } return -1; },
    __slice = [].slice,
    __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; };

  define(function(require) {
    var ChooseDialogueCtrl, Controllers, HistoryController, L, atURL, ga, inject, injectables, ng, overlaps, toTool;
    ng = require('angular');
    L = require('lodash');
    ga = require('analytics');
    require('services');
    ChooseDialogueCtrl = require('./choose-dialogue');
    injectables = L.pairs({
      scope: '$scope',
      route: '$route',
      http: '$http',
      location: '$location',
      params: '$routeParams',
      to: '$timeout',
      console: '$log',
      Q: '$q',
      $modal: '$modal',
      connectTo: 'connectTo',
      meetRequest: 'meetRequest',
      Histories: 'Histories',
      Mines: 'Mines',
      silentLocation: '$ngSilentLocation',
      serviceStamp: 'serviceStamp',
      notify: 'notify'
    });
    overlaps = function(x, y) {
      return x && y && (x.indexOf(y) >= 0 || y.indexOf(x) >= 0);
    };
    atURL = function(url) {
      return function(mines) {
        return L.find(mines, function(m) {
          return overlaps(m.root, url);
        });
      };
    };
    toTool = function(conf) {
      var handles, providers;
      handles = conf.handles;
      providers = conf.provides;
      if (Array.isArray(handles)) {
        conf.handles = function(cat) {
          return handles.indexOf(cat) >= 0;
        };
      } else {
        conf.handles = function(cat) {
          return handles === cat;
        };
      }
      if (Array.isArray(providers)) {
        conf.provides = function(x) {
          return __indexOf.call(providers, x) >= 0;
        };
      } else {
        conf.provides = function(x) {
          return x === providers;
        };
      }
      return conf;
    };
    inject = function(fn) {
      return function() {
        var idx, injected, name, _, _i, _len, _ref;
        injected = 1 <= arguments.length ? __slice.call(arguments, 0) : [];
        for (idx = _i = 0, _len = injectables.length; _i < _len; idx = ++_i) {
          _ref = injectables[idx], name = _ref[0], _ = _ref[1];
          this[name] = injected[idx];
        }
        return fn.call(this);
      };
    };
    Controllers = ng.module('steps.history.controllers', ['steps.services']);
    Controllers.controller('HistoryCtrl', HistoryController = (function() {
      var fst, snd, _class;

      function HistoryController() {
        this.nextStep = __bind(this.nextStep, this);
        return _class.apply(this, arguments);
      }

      HistoryController.prototype.currentCardinal = 0;

      HistoryController.$inject = (function() {
        var _i, _len, _ref, _results;
        _results = [];
        for (_i = 0, _len = injectables.length; _i < _len; _i++) {
          _ref = injectables[_i], fst = _ref[0], snd = _ref[1];
          _results.push(snd);
        }
        return _results;
      })();

      _class = inject(function() {
        this.init();
        this.startWatching();
        return console.log(this);
      });

      HistoryController.prototype.elide = true;

      HistoryController.prototype.startWatching = function() {
        var scope;
        scope = this.scope;
        scope.$watchCollection('items', function() {
          var data, exporters, key, otherSteps, s, tool, _i, _len, _ref, _ref1;
          exporters = [];
          _ref = scope.nextTools;
          for (_i = 0, _len = _ref.length; _i < _len; _i++) {
            tool = _ref[_i];
            if (tool.handles('items')) {
              _ref1 = scope.items;
              for (key in _ref1) {
                data = _ref1[key];
                if (data.ids.length) {
                  exporters.push({
                    key: key,
                    data: data,
                    tool: tool
                  });
                }
              }
            }
          }
          otherSteps = (function() {
            var _j, _len1, _ref2, _results;
            _ref2 = scope.nextSteps;
            _results = [];
            for (_j = 0, _len1 = _ref2.length; _j < _len1; _j++) {
              s = _ref2[_j];
              if (!s.tool.handles('items')) {
                _results.push(s);
              }
            }
            return _results;
          })();
          scope.nextSteps = otherSteps.concat(exporters);
          return console.log("NEXT STEPS (items)", scope.nextSteps);
        });
        scope.$watch('messages', function(msgs) {
          var handlers, otherSteps, s;
          handlers = L.values(msgs);
          otherSteps = (function() {
            var _i, _len, _ref, _results;
            _ref = scope.nextSteps;
            _results = [];
            for (_i = 0, _len = _ref.length; _i < _len; _i++) {
              s = _ref[_i];
              if (s.kind !== 'msg') {
                _results.push(s);
              }
            }
            return _results;
          })();
          scope.nextSteps = otherSteps.concat(handlers);
          return console.log("NEXT STEPS (msg)", scope.nextSteps);
        });
        return scope.$watch('list', function() {
          var categories, listHandlers, otherSteps, s, tool, _i, _j, _len, _len1, _ref, _ref1, _ref2;
          listHandlers = [];
          _ref = scope.nextTools;
          for (_i = 0, _len = _ref.length; _i < _len; _i++) {
            tool = _ref[_i];
            if (tool.handles('list')) {
              listHandlers.push({
                tool: tool,
                data: scope.list
              });
            }
          }
          otherSteps = (function() {
            var _j, _len1, _ref1, _results;
            _ref1 = scope.nextSteps;
            _results = [];
            for (_j = 0, _len1 = _ref1.length; _j < _len1; _j++) {
              s = _ref1[_j];
              if (!s.tool.handles('list')) {
                _results.push(s);
              }
            }
            return _results;
          })();
          console.log("listHandlers", listHandlers);
          scope.nextSteps = otherSteps.concat(listHandlers);
          categories = [];
          scope.nextSteps2 = [];
          _ref1 = scope.nextTools;
          for (_j = 0, _len1 = _ref1.length; _j < _len1; _j++) {
            tool = _ref1[_j];
            if ((tool.category != null) && (_ref2 = tool.category, __indexOf.call(categories, _ref2) < 0)) {
              categories.push(tool.category);
              scope.nextSteps2.push(tool);
            }
          }
          categories.push("Other");
          console.log("nextsteps2", scope.nextSteps2);
          return scope.categories = categories;
        });
      };

      HistoryController.prototype.init = function() {
        var Histories, Mines, http, params, toolNotFound, _base, _base1, _base2, _base3, _base4;
        Histories = this.Histories, Mines = this.Mines, params = this.params, http = this.http;
        if ((_base = this.scope).nextTools == null) {
          _base.nextTools = [];
        }
        if ((_base1 = this.scope).providers == null) {
          _base1.providers = [];
        }
        if ((_base2 = this.scope).messages == null) {
          _base2.messages = {};
        }
        if ((_base3 = this.scope).nextSteps == null) {
          _base3.nextSteps = [];
        }
        if ((_base4 = this.scope).items == null) {
          _base4.items = {};
        }
        this.scope.collapsed = true;
        this.scope.state = {
          expanded: false,
          nextStepsCollapsed: true
        };
        this.currentCardinal = parseInt(params.idx, 10);
        this.scope.history = Histories.get({
          id: params.id
        });
        this.scope.steps = Histories.getSteps({
          id: params.id
        });
        this.scope.step = Histories.getStep({
          id: params.id,
          idx: this.currentCardinal - 1
        });
        this.mines = Mines.all();
        this.scope.showtools = (function(_this) {
          return function(val) {
            var item, s, _i, _len, _ref;
            console.log("SHOWING TOOLS");
            console.log(_this.scope.nextSteps);
            console.log("SCOPE.STEPS", _this.scope.steps);
            console.log("SCOPE.HISTORIES", _this.scope.steps);
            if (val !== "Other") {
              _this.scope.nextSteps2 = (function() {
                var _i, _len, _ref, _results;
                _ref = this.scope.nextSteps;
                _results = [];
                for (_i = 0, _len = _ref.length; _i < _len; _i++) {
                  s = _ref[_i];
                  if (s.tool.category === val) {
                    _results.push(s);
                  }
                }
                return _results;
              }).call(_this);
            } else {
              _ref = _this.scope.nextSteps;
              for (_i = 0, _len = _ref.length; _i < _len; _i++) {
                item = _ref[_i];
                console.log("val", item.tool.category);
              }
              _this.scope.nextSteps2 = (function() {
                var _j, _len1, _ref1, _results;
                _ref1 = this.scope.nextSteps;
                _results = [];
                for (_j = 0, _len1 = _ref1.length; _j < _len1; _j++) {
                  s = _ref1[_j];
                  if (s.tool.category == null) {
                    _results.push(s);
                  }
                }
                return _results;
              }).call(_this);
            }
            return console.log("next steps is now", _this.scope.nextSteps2);
          };
        })(this);
        this.scope.showmenu = (function(_this) {
          return function() {
            console.log("showing menu");
            return _this.scope.showsubmenu = true;
          };
        })(this);
        this.scope.hidemenu = (function(_this) {
          return function() {
            console.log("hiding menu");
            return _this.scope.showsubmenu = false;
          };
        })(this);
        this.scope.expandhistory = (function(_this) {
          return function() {
            console.log("showing history");
            return _this.scope.openhistory = true;
          };
        })(this);
        this.scope.shrinkhistory = (function(_this) {
          return function() {
            console.log("hiding history");
            return _this.scope.openhistory = false;
          };
        })(this);
        toolNotFound = (function(_this) {
          return function(e) {
            return _this.to(function() {
              return _this.scope.error = e;
            });
          };
        })(this);
        if (this.scope.tool == null) {
          this.scope.step.$promise.then((function(_this) {
            return function(_arg) {
              var tool;
              tool = _arg.tool;
              http.get('/tools/' + tool).then((function(_arg1) {
                var data;
                data = _arg1.data;
                return _this.scope.tool = data;
              }), toolNotFound);
              return http.get('/tools', {
                params: {
                  capabilities: 'next'
                }
              }).then(function(_arg1) {
                var data;
                data = _arg1.data;
                return _this.scope.nextTools = data.map(toTool);
              });
            };
          })(this));
        }
        return http.get('/tools', {
          params: {
            capabilities: 'provider'
          }
        }).then(function(_arg) {
          var data;
          data = _arg.data;
          return data.map(toTool);
        }).then((function(_this) {
          return function(providers) {
            return _this.scope.providers = providers;
          };
        })(this));
      };

      HistoryController.prototype.saveHistory = function() {
        this.scope.editing = false;
        return this.scope.history.$save();
      };

      HistoryController.prototype.updateHistory = function() {
        var Histories, params;
        Histories = this.Histories, params = this.params;
        this.scope.editing = false;
        return this.scope.history = Histories.get({
          id: params.id
        });
      };

      HistoryController.prototype.setItems = function() {
        return (function(_this) {
          return function(key, type, ids) {
            return _this.set(['items', key], {
              type: type,
              ids: ids
            });
          };
        })(this);
      };

      HistoryController.prototype.set = function(_arg, value) {
        var key, prekeys, _i;
        prekeys = 2 <= _arg.length ? __slice.call(_arg, 0, _i = _arg.length - 1) : (_i = 0, []), key = _arg[_i++];
        return this.to((function(_this) {
          return function() {
            var i, k, o, _j, _len;
            o = _this.scope;
            for (i = _j = 0, _len = prekeys.length; _j < _len; i = ++_j) {
              k = prekeys[i];
              o = o[k];
            }
            return o[key] = value;
          };
        })(this));
      };

      HistoryController.prototype.hasSomething = function(what, data, key) {
        var Q, console, handlers, mines, scope, to, tool, triggerUpdate, _fn, _i, _len;
        scope = this.scope, console = this.console, to = this.to, Q = this.Q, mines = this.mines;
        if (what === 'list') {
          return to(function() {
            return scope.list = data;
          });
        }
        triggerUpdate = function() {
          return to(function() {
            return scope.messages = L.assign({}, scope.messages);
          });
        };
        handlers = (function() {
          var _i, _len, _ref, _results;
          _ref = scope.nextTools;
          _results = [];
          for (_i = 0, _len = _ref.length; _i < _len; _i++) {
            tool = _ref[_i];
            if (tool.handles(what)) {
              _results.push(tool);
            }
          }
          return _results;
        })();
        _fn = (function(_this) {
          return function(tool) {
            var idx, _ref;
            idx = tool.ident + what + key;
            if (data) {
              if (scope.messages[idx] != null) {
                return _this.set(['messages', idx, 'data'], data);
              } else {
                return _this.connectTo((_ref = data['service:base']) != null ? _ref : data.service.root).then(function(service) {
                  _this.set(['messages', idx], {
                    tool: tool,
                    data: data,
                    service: service,
                    kind: 'msg'
                  });
                  return triggerUpdate();
                });
              }
            } else {
              return delete scope.messages[idx];
            }
          };
        })(this);
        for (_i = 0, _len = handlers.length; _i < _len; _i++) {
          tool = handlers[_i];
          _fn(tool);
        }
        if (handlers.length) {
          return triggerUpdate();
        }
      };

      HistoryController.prototype.letUserChoose = function(tools) {
        var dialogue;
        dialogue = this.$modal.open({
          templateUrl: '/partials/choose-tool-dialogue.html',
          controller: ChooseDialogueCtrl,
          resolve: {
            items: tools
          }
        });
        return dialogue.result;
      };

      HistoryController.prototype.meetingRequest = function(next, data) {
        if (next.length === 1) {
          return this.meetRequest(next[0], this.scope.step, data);
        } else {
          return this.letUserChoose(next).then((function(_this) {
            return function(provider) {
              return _this.meetRequest(provider, _this.scope.step, data);
            };
          })(this));
        }
      };

      HistoryController.prototype.wantsSomething = function(what, data) {
        var console, meetRequest, next, notify;
        notify = this.notify, console = this.console, meetRequest = this.meetRequest;
        console.log("Something is wanted", what, data);
        next = this.scope.providers.filter(function(t) {
          return t.provides(what);
        });
        console.log("Suitable providers found", next, this.scope.providers);
        if (!next.length) {
          return;
        }
        return this.meetingRequest(next, data).then(this.nextStep, function(err) {
          console.error(err);
          return notify(err.message);
        });
      };

      HistoryController.prototype.storeHistory = function(step) {
        return this.nextStep(step, true);
      };

      HistoryController.prototype.stampStep = function(step) {
        var Q, serviceStamp;
        Q = this.Q, serviceStamp = this.serviceStamp;
        if (step.data.service == null) {
          return Q.when(step);
        } else {
          return serviceStamp(step.data.service).then(function(stamp) {
            return L.assign({
              stamp: stamp
            }, step);
          });
        }
      };

      HistoryController.prototype.nextStep = function(step, silently) {
        var Histories, appended, console, currentCardinal, goTo, history, location, nextCardinal, scope, silentLocation;
        if (silently == null) {
          silently = false;
        }
        Histories = this.Histories, scope = this.scope, currentCardinal = this.currentCardinal, console = this.console, location = this.location, silentLocation = this.silentLocation;
        console.debug("Next step:", step);
        console.log("storing - silently? " + silently);
        nextCardinal = currentCardinal + 1;
        appended = null;
        goTo = silently ? (function(_this) {
          return function(url) {
            _this.params.idx = nextCardinal;
            silentLocation.silent(url);
            _this.currentCardinal = nextCardinal;
            return _this.init();
          };
        })(this) : function(url) {
          return location.url(url);
        };
        history = scope.history;
        return this.stampStep(step).then(function(stamped) {
          var fork, title;
          console.log('STAMPED', stamped);
          if (history.steps.length !== currentCardinal) {
            title = "Fork of " + history.title;
            console.debug("Forking at " + currentCardinal + " as " + title);
            return fork = Histories.fork({
              id: history.id,
              at: currentCardinal,
              title: title
            }, function() {
              console.debug("Forked history");
              return appended = Histories.append({
                id: fork.id
              }, stamped, function() {
                console.debug("Created step " + appended.id);
                return goTo("/history/" + fork.id + "/" + nextCardinal);
              });
            });
          } else {
            return appended = Histories.append({
              id: history.id
            }, stamped, function() {
              console.debug("Created step " + appended.id);
              goTo("/history/" + history.id + "/" + nextCardinal);
              return ga('send', 'event', 'history', 'append', step.tool);
            });
          }
        });
      };

      return HistoryController;

    })());
    return Controllers.controller('HistoryStepCtrl', Array('$scope', '$log', '$modal', function(scope, log, modalFactory) {
      var InputEditCtrl;
      scope.$watch('appView.elide', function() {
        return scope.elide = scope.appView.elide && scope.$middle && (scope.steps.length - scope.$index) > 3;
      });
      InputEditCtrl = Array('$scope', '$modalInstance', 'history', 'step', function(scope, modal, history, step, index) {
        scope.data = L.clone(step.data, true);
        delete scope.data.service;
        scope.ok = function() {
          return modal.close(history.id, index, scope.data);
        };
        return scope.cancel = function() {
          modal.dismiss('cancel');
          return scope.data = L.clone(step.data, true);
        };
      });
      return scope.openEditDialogue = function() {
        var dialogue;
        return dialogue = modalFactory.open({
          templateUrl: '/partials/edit-step-data.html',
          controller: InputEditCtrl,
          size: 'lg',
          resolve: {
            index: function() {
              return scope.$index;
            },
            history: function() {
              return scope.history;
            },
            step: function() {
              return scope.s;
            }
          }
        });
      };
    }));
  });

}).call(this);
