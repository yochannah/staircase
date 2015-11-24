
ALPHABET = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split('')
TOO_MANY_CONS = "Cannot add this constraint - the query would exceed the maximum number off constraints"

define ['lodash'], (L) ->



  decap = (str) -> str.replace /^[^\.]*/, ''

  replaceCode = do ->
    obscured = AND: '1', OR: '2' # logic expressions do not contain numbers
    plain = L.invert obscured
    from = (mapping) -> (key) -> mapping[key]
    findKeys = (mapping) -> new RegExp "(#{ L.keys(mapping).join('|') })", 'g'
    (logicExpr, code, replacements) ->
      logicExpr.replace(findKeys(obscured), from obscured)
              .replace(new RegExp(code, 'g'), " (#{ replacements.join(' AND ') }) ")
              .replace(findKeys(plain), from plain)

  # Make the constraints suitable for applying to the query.
  # This means adjusting their paths, and giving them suitable codes.
  adjustForQuery = (cons, unavailableCodes, conPath) ->
    highestCode = unavailableCodes.slice().sort().pop()
    if unavailableCodes.length and not highestCode?
      debugger
    nextIdx = ALPHABET.indexOf(highestCode) + 1
    getCode = ->
      if nextIdx >= ALPHABET.length
        throw new Error(TOO_MANY_CONS)
      nextCode = ALPHABET[nextIdx]
      nextIdx += 1
      return nextCode
    for newCon in cons # returns the adjusted constraints.
      path = if newCon.path then conPath + newCon.path else conPath
      code = getCode()
      L.assign newCon, {path, code}

  getTargetConstraint = (q) ->
    [first] = editables = (c for c in q.constraints when c.editable)
    if editables.length is 1
      return first # One editable constraint
    else
      # One lookup, and we switch the others off.
      lookup = c for c in editables when c.op is 'LOOKUP'
      others = (c for c in editables when c.op isnt 'LOOKUP')
      for o in others
        o.switched = 'OFF'
      return lookup

  applyConstraintsToQuery = (q, cons) ->
    # Get the only editable constraint - part of the contract.
<<<<<<< HEAD
    #console.log '----', q.name
=======
    # console.log '----', q.name
>>>>>>> nextsteps
    con = getTargetConstraint q
    path = q.makePath con.path

    conPath = if path.isAttribute() then path.getParent().toString() else con.path
    if cons.length is 1
      # Fortunate case - just replace the values of the actual constraint.
      newCon = cons[0]
      delete con.value
      delete con.values
      L.assign con, newCon
      if newCon.path # concat rather than replacement.
        con.path = conPath + newCon.path
    else
      # Calculate the new constraints and logic
      unavailableCodes = (c.code for c in q.constraints when c.code? and c isnt con)
      newCons = adjustForQuery cons, unavailableCodes, conPath
      codesAdded = (c.code for c in newCons)
      newLogic = replaceCode q.constraintLogic, con.code, codesAdded
<<<<<<< HEAD
      #console.log q.name, "The new constraints are", newCons
      #console.log q.name, "The new codes are", codesAdded
=======
      # console.log q.name, "The new constraints are", newCons
      # console.log q.name, "The new codes are", codesAdded
>>>>>>> nextsteps

      # Apply changes
      q.removeConstraint con.code
      q.addConstraints newCons
      q.constraintLogic = newLogic

  inject = ['$timeout', '$log', '$q', '$scope', 'identifyItem', 'identifyItems']
  Array inject..., (to, console, Q, scope, identifyItem, identifyItems) ->

    scope.runTemplate = ->
<<<<<<< HEAD
      #console.log "this is", @
=======
      # console.log "this is", @
>>>>>>> nextsteps
      scope.run scope.query if scope.query?


    getReplacementConstraints = if scope.list?
      #console.debug "Running over #{ scope.list.name }"
      Q.when [{op: 'IN', value: scope.list.name}]
    else if scope.items?.ids
      #console.debug "Identifying items at #{ scope.service.root }"
      if scope.items.ids.length is 1
        identifyItem(scope.service, type: scope.items.type, id: scope.items.ids[0]).then (fields) ->
          ({path: decap(path), op: '==', value} for path, value of fields)
      else
        identifyItems(scope.service, scope.items).then (fields) ->
          ({path: decap(path), op: 'ONE OF', values} for path, values of fields)
    else
      Q.reject 'Cannot generate constraints - list or items are required'

    Q.all([scope.service.query(scope.template), getReplacementConstraints]).then ([q, cons]) ->
      #console.debug "Constraints are", cons
      applyConstraintsToQuery q, cons

      to -> scope.query = q
      q.count().then (n) -> to -> scope.count = n
