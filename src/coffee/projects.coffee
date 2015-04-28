define (require) ->

  # This module serves to require the various bits that make up the projects
  # modules.
  ng = require 'angular'
  require 'projects/services'
  require 'projects/controllers'
  
  ng.module 'steps.projects', ['steps.projects.controllers', 'steps.projects.services']

