deps = ['angular', 'filters', 'services', 'directives', 'controllers']

modules = ['steps.controllers', 'steps.filters', 'steps.services', 'steps.directives']

define deps, (angular) ->
  
  Steps = angular.module('steps', modules)

  return Steps
