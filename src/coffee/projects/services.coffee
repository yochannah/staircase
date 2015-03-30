define (require) ->

  L = require 'lodash'
  ng = require 'angular'
  require 'services' # provides steps.services

  ProjectServices = ng.module('steps.projects.services', ['steps.services'])

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

  ProjectServices.factory 'getMineUserEntities', Array 'imjs', 'Mines', '$q', (imjs, Mines, Q) ->
    asEntity = (mine, type, id, entity) ->
      type: type
      id: id
      entity: entity
      source: mine.name
      meta: mine.meta

    mergeArrays = (a, b) -> a.concat(b) if L.isArray a
    reduceFn = (m, es) -> L.merge m, es, mergeArrays

    return getMineUserEntities = -> Mines.all().then (mines) ->
      promises = mines.map (amine) ->
        service = imjs.Service.connect amine

        getLists = service.fetchLists().then (lists) -> L.map lists, (list) ->
          asEntity amine, 'List', list.name, list
        getTemplates = service.fetchTemplates().then (ts) -> L.map ts, (t) ->
          asEntity amine, 'Template', t.name, t

        Q.all([getLists, getTemplates]).then ([lists, templates]) ->
          {lists, templates}

      # Return a promise for a single combined entity set.
      Q.all(promises).then (entitySets) ->
        L.reduce entitySets, reduceFn, {lists: [], templates: []}
