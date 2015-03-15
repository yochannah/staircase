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

  OPERATIONS = [ # lefts, common, rights used to produce a schematic diagram.
    {op: 'intersect', verb: 'intersect with', common: true }
    {op: 'merge', verb: 'merge with', lefts: true, common: true, rights: true}
    {op: 'complement', verb: 'remove elements shared with', lefts: true}
    {op: 'complement', verb: 'remove from', rev: true, rights: true}
    {op: 'diff', verb: 'calculate symmetric difference with', lefts: true, rights: true}
  ]

  ALPHABET = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split('')

  controller = (scope, modal, Q, generateListName, service, list, lists, model) ->

    scope.classNames = {}
    scope.operations = OPERATIONS
    scope.operation = OPERATIONS[0]
    scope.otherLists = []
    scope.counts = {}

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

    scope.$watch 'otherLists', (others) ->
      unless others?.length
        return scope.counts = {}

      sync = L.now()

      inOthers = ({path: 'InterMineObject', op: 'IN', value: o} for o in others)
      inThis = path: 'InterMineObject', op: 'IN', value: list.name

      leftQ =
        select: ['InterMineObject.id']
        where: [inThis].concat(L.defaults({op: 'NOT IN'}, o) for o in inOthers)

      commonQ =
        select: ['InterMineObject.id']
        where: [inThis].concat(inOthers)

      rightQ =
        select: ['InterMineObject.id']
        where: [L.defaults({op: 'NOT IN'}, inThis)].concat(inOthers)
        constraintLogic: "A and (#{ (ALPHABET[i + 1] for o, i in others).join ' OR ' })"

      # Ignore the results if a more recent request has been made.
      handler = do (mySync = sync) -> ([left, common, right]) ->
        return if sync > mySync
        scope.counts = {left, common, right}

      Q.all(service.count q for q in [leftQ, commonQ, rightQ]).then handler

    scope.cancel = -> modal.dismiss 'cancel'
    scope.run = ->
      operation = scope.operation
      opts =
        name: scope.newListName
        description: scope.description

      # Complement is not commutative, the others are.
      if operation.op is 'complement'
        [them, us] = [scope.otherLists, [list.name]]
        if operation.rev
          [us, them] = [them, us]
        opts.from = us
        opts.exclude = them
      else
        opts.lists = scope.otherLists.concat([list.name])

      console.log opts

      service[operation.op](opts).then (combo) -> modal.close combo
  
  return [inject..., controller]
