define ['imjs'], ({Service}) -> Array '$scope', 'Mines', (scope, Mines) ->

  Mines.get('default')
       .then(Service.connect)
       .then((conn) -> conn.fetchTemplates())
       .then (ts) -> scope.$apply -> scope.templates = (t for _, t of ts)

