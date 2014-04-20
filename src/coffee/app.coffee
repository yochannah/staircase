deps = ['angular', 'filters', 'services', 'directives', 'controllers', 'angular-ui']

modules = [
  'steps.controllers',
  'steps.filters',
  'steps.services',
  'steps.directives',
  'ui.bootstrap']

define deps, (angular) -> angular.module('steps', modules)
