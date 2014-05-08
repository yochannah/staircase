define([], function () {

  return ['$scope', '$interval', 'Histories', HistoryToolController];

  function HistoryToolController (scope, interval, Histories) {
    scope.histories = Histories.query();

    // Update once a minute.
    var autoUpdate = interval(function () {}, 60 * 1000);

    scope.$on('$destroy', function () {
      interval.cancel(autoUpdate);
    });

    scope.deleteHistory = function (history) {
      history.$delete(function () {
        scope.histories = Histories.query();
      });
    };
  }

});
