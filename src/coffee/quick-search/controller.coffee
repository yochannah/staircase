define (require) ->

  ng = require 'angular'
  require 'services'

  Controllers = ng.module 'steps.quick-search.controllers', ['steps.services']

  Controllers.controller 'QuickSearchController', class QuickSearchController
  
    @$inject = ['$log', '$scope', 'historyListener']
    
    constructor: (log, scope, {startHistory}) ->
      scope.startQuickSearchHistory = (term) ->
        startHistory
          thing: "for #{ term }"
          verb:
            ed: "searched"
            ing: "searching"
          tool: "keyword-search"
          data:
            searchTerm: term

