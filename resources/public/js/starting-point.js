(function() {
  define(function(require) {
    var ng;
    ng = require('angular');
    require('starting-point/controller');
    return ng.module('steps.starting-point', ['steps.starting-point.controller']);
  });

}).call(this);
