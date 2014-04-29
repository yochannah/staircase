define([], function () {

  return ['$scope', 'Histories', HistoryToolController];

  function HistoryToolController (scope, Histories) {
    Histories.then(function (histories) {
      scope.histories = histories.query();
    });
  }

});
