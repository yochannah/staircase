define (require, module, exports) ->

  L = require 'lodash'

  isSuitableForMergingWith = ({type, name}, model) -> (list) ->
    return false if name is list.name # Don't combine with self
    return false if not list.size # Any operations with empty lists are pointless
    # Operations are associative, so inheritance must be too, so
    # we can combine Managers with Employees and Employees with Managers
    model.makePath(list.type).isa(type) or model.makePath(type).isa(list.type)

  # The modal launcher must give use this list and the other lists.
  inject = ['$scope', '$modalInstance', '$q', 'generateListName', 'service', 'list', 'lists', 'model']

  OPERATIONS = [
    {op: 'merge', verb: 'merge with'}
    {op: 'intersect', verb: 'intersect with'}
    {op: 'complement', verb: 'remove elements shared with'}
    {op: 'diff', verb: 'calculate symmetric difference with'}
  ]

  controller = (scope, modal, Q, generateListName, service, list, lists, model) ->

    scope.classNames = {}
    scope.operations = OPERATIONS
    scope.operation = OPERATIONS[0]
    scope.descLimit = 120 # characters. Tweet sized is best.

    scope.service = service
    scope.list = list

    isSuitable = isSuitableForMergingWith list, model
    scope.lists = lists.filter isSuitable
    # scope.otherLists = [scope.lists[0].name]

    generateListName(service, list.type, 'Combination').then (name) ->
      scope.newListName ?= name

    promises = {} # make sure we only process each type once.
    for {type} in scope.lists then do (type) ->
      promises[type] ?= Q.when(model.makePath(type))
                         .then (p) -> p.getDisplayName()
                         .then (name) -> scope.classNames[type] = name

    scope.cancel = -> modal.dismiss 'cancel'
    scope.run = ->
      opts =
        name: scope.newListName
        description: scope.description

      # Complement is not commutative, the others are.
      if scope.operation.op is 'complement'
        opts.from = [list.name]
        opts.exclude = scope.otherLists
      else
        opts.lists = scope.otherLists.concat([list.name])

      console.log opts

      service[scope.operation.op](opts).then (combo) -> modal.close combo
  
  return [inject..., controller]
