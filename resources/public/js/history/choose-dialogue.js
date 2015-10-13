(function() {
  define([], function() {
    return Array('$scope', '$modalInstance', 'items', function(scope, dialogue, items) {
      scope.items = items;
      scope.selected = {
        item: item[0]
      };
      scope.ok = function() {
        return dialogue.close(scope.selected.item);
      };
      return scope.cancel = function() {
        return dialogue.dismiss('cancel');
      };
    });
  });

}).call(this);
