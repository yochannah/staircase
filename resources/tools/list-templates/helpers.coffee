define ['lodash', 'imjs'], (L, {Query}) ->

  getParsedTitle = ({title, name}) -> (title or name).replace /.*--> /, ''

  isSuitableForType = (type, model) -> (template) ->
    query = new Query L.extend {}, template, {model}
    return false if not query.views.length
    [con, conN] = editables = L.where(query.constraints, editable: true)
    return false if (not con)

    if conN # We can handle some classes of multiple constraints.
      lookups = L.where(editables, op: 'LOOKUP')
      if lookups.length isnt 1
        # console.log "Template #{ template.name } isnt suitable because it doesn't have one lookup", template
        return false
      others = (c for c in editables when c.op isnt 'LOOKUP')
      unless L.all(others, (c) -> c.switchable)
        # console.log  "Template #{ template.name } isnt suitable because the other editable are not switchable", template
        return false

    path = query.makePath con.path
    path = path.getParent() if path.isAttribute()
    path.isa type

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
    # console.log '----', q.name
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
      # console.log q.name, "The new constraints are", newCons
      # console.log q.name, "The new codes are", codesAdded

      # Apply changes
      q.removeConstraint con.code
      q.addConstraints newCons
      q.constraintLogic = newLogic


  return {isSuitableForType, getParsedTitle}
