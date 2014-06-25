define [], -> Array '$scope', (scope) ->

  scope.runTemplate = -> scope.run scope.query if scope.query?
 
  scope.service.query(scope.template).then (q) ->
    con = c for c in q.constraints when c.editable
    path = q.makePath con.path
    
    con.path = path.getParent().toString() if path.isAttribute()
    con.op = 'IN'
    con.value = scope.list.name

    scope.$apply -> scope.query = q
    q.count().then (n) -> scope.$apply -> scope.count = n

