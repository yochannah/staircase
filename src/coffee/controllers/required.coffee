define [], -> Array '$scope', '$http', ($scope, $http) ->

  # Need to call scope.$apply in required controllers since they 
  # missed out on the initial call to apply from the main angular module.
  $scope.$apply()
