define([], function () {

  return ['$scope', 'Histories', HistoryToolController];

  function HistoryToolController (scope, Histories) {
    console.log(scope, Histories);
    scope.histories = Histories.query();
  }

});
