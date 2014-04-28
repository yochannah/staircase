define([], function () {

  return ['$scope', 'Histories', HistoryToolController];

  function HistoryToolController (scope, Histories) {
    scope.histories = Histories.query();
  }

});
