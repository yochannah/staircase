define (require) ->

  ng = require 'angular'
  require 'services' # provides steps.services
  require 'mines/services' # provides steps.mines.services

  ProjectServices = ng.module('steps.projects.services', [
    'steps.services', 'steps.mines.services'
  ])

  # API endpoint adapter for Projects.
  ProjectServices.factory 'Projects', Array 'WebServiceAuth', '$http', '$resource', (auth, http, resource) ->
    headers = auth.headers # This object is updated with the latest auth data - DO NOT CLONE.
    resource "/api/v1/projects/:projectId", {projectId: '@projectId'},
      get: {method: 'GET', headers: headers}
      query: {method: 'GET', headers: headers, isArray: true} # Angularism
      all: {method: 'GET', headers: headers, isArray: true} # Preferred alias
      create: {method: 'POST', headers: headers}
      save: {method: 'PUT', headers: headers} # Angularism
      update: {method: 'PUT', headers: headers} # Preferred alias
      delete: {method: 'DELETE', headers: headers} # ng-ish to have both.
      remove: {method: 'DELETE', headers: headers} # Alias
      getItem: {method: 'GET', headers: headers, url: '/api/v1/projects/:projectId/items/:itemId'}
      removeItem: {method: 'DELETE', headers: headers, url: '/api/v1/projects/:projectId/items/:itemId'}
      deleteItem: {method: 'DELETE', headers: headers, url: '/api/v1/projects/:projectId/items/:itemId'}
      addItem: {method: 'POST', headers: headers, url: '/api/v1/projects/:projectId/items'}

