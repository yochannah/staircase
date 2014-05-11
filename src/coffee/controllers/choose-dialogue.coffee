define [], -> Array '$scope', '$modalInstance', 'items', (scope, dialogue, items) ->

  scope.items = items
  scope.selected = {item: item[0]}

  scope.ok = -> dialogue.close(scope.selected.item)

  scope.cancel = -> dialogue.dismiss('cancel')
