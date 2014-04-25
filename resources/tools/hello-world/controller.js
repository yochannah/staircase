define([], function () {
  return ['$scope', '$timeout', 'version', function (scope, timeout, version) {
    scope.place = "Cambridge";
    scope.stepsVersion = version;
    scope.staircaseVersion = version;

    scope.tool.heading = "Hello " + scope.place;
  }];
});

