define([], function () {
  return ['$scope', '$timeout', 'version', function (scope, timeout, version) {
    scope.place = "Cambridge";
    scope.stepsVersion = version;
    scope.staircaseVersion = version;

    scope.tool.heading = "Hello " + scope.place;

    scope.items = [
      {name:"Gene ontology"},
      {name:"Pathways"},
      {name:"Protein Domain"},
      {name:"Literature"},
      {name:"Expression"},
      {name:"Gene ontology"},
      {name:"Pathways"},
      {name:"Protein Domain"},
      {name:"Literature"},
      {name:"Expression"},
      {name:"Gene ontology"},
      {name:"Pathways"},
      {name:"Protein Domain"},
      {name:"Literature"},
      {name:"Expression"}
    ];


  }];
});
