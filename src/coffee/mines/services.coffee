define (require) ->

  L = require 'lodash'
  ng = require 'angular'
  require 'services' # provides steps.services

  MineServices = ng.module('steps.mines.services', ['steps.services'])

  MineServices.factory 'getMineUserEntities', Array 'imjs', 'Mines', '$q', (imjs, Mines, Q) ->
    asEntity = (mine, type, id, entity) ->
      type: type
      id: id
      entity: entity
      source: mine.name
      meta: mine.meta

    mergeArrays = (a, b) -> a.concat(b) if L.isArray a
    reduceFn = (m, es) -> L.merge m, es, mergeArrays
    # Never fail - always return an empty list - this prevents failing to
    # return any entities when only one service is down/unreliable.
    recover = (err) -> Q.when errors: [err]
    mergeEntitySets = (entitySets) ->
      L.reduce entitySets, reduceFn, {errors: [], lists: [], templates: []}

    return getMineUserEntities = -> Mines.all().then (mines) ->
      promises = mines.map (amine) ->
        service = imjs.Service.connect amine

        handleLists = (lists) -> lists: L.map lists, (list) ->
          asEntity amine, 'List', list.name, list
        handleTemplates =  (ts) -> templates: L.map ts, (t) ->
          asEntity amine, 'Template', t.name, t

        getLists = service.fetchLists().then handleLists, recover
        getTemplates = service.fetchTemplates().then handleTemplates, recover

        Q.all([getLists, getTemplates]).then mergeEntitySets

      # Return a promise for a single combined entity set.
      Q.all(promises).then mergeEntitySets
