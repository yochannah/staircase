define (require) ->

  ng = require 'angular'
  require 'starting-point/controller'

  ng.module 'steps.starting-point', ['steps.starting-point.controller']
