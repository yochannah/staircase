define ['imjs', 'lodash'], ({Service}, L) ->

  # We need to specify the taxon-id for each segment, otherwise the results _will_ be wrong.
  toFullRegions = (rows) -> ("#{taxon}:#{chr}:#{s}..#{e}" for [chr, s, e, taxon] in rows)
  
  Array '$q', '$log', '$scope', 'Mines', 'ClassUtils', (Q, console, scope, mines, ClassUtils) ->

    # :: RegionSet {root :: string,
    #               targetClass :: {name :: string, path :: Path},
    #               summary :: string, 
    #               fetch :: () -> Promise [[object]]}
    scope.regionset = {}
    # :: [RegionSet]
    scope.regionsets = []
    # :: [{name :: string, path :: Path }]
    scope.classes = []
    name = scope.data.name
    type = scope.data.type

    getService = mines.atURL(scope.data.root or scope.service.root)
                      .then(Service.connect)

    getModel = getService.then (service) -> service.fetchModel()

    getClasses = getModel.then ClassUtils.getSubTypesOf 'SequenceFeature'

    Q.all([getService, getModel, getClasses]).then ([service, model, classes]) ->
      [targetClass] = scope.classes = classes
      path = model.makePath(type)

      if path.isa('Location')
        scope.regionsets.push
          root: service.root,
          targetClass: targetClass
          summary: 'these regions',
          fetch: -> service.rows
            select: ['locatedOn.primaryIdentifier', 'start', 'end', 'locatedOn.organism.taxonId']
            from: type
            orderBy: [
              'locatedOn.organism.taxonId', 'locatedOn.primaryIdentifier',
              'start', 'end'
            ]
            where: [[type, 'IN', name]]
      else if path.isa('SequenceFeature')
        scope.regionsets.push
          root: service.root,
          targetClass: targetClass
          summary: "these #{ type }s",
          fetch: -> service.rows
            select: [
              'chromosome.primaryIdentifier', 'chromosomeLocation.start', 'chromosomeLocation.end',
              'organism.taxonId'
            ],
            from: type,
            orderBy: [
              'organism.taxonId', 'chromosome.primaryIdentifier',
              'chromosomeLocation.start', 'chromosomeLocation.end'
            ]
            where: [[type, 'IN', name]]

      scope.regionset = scope.regionsets[0]


    scope.activate = (regionset) ->
      regionset.fetch().then (rows) ->
        request =
          regions: toFullRegions rows
          types: [regionset.targetClass.path.toString()]
        nextStep =
          title: 'Search for overlapping features',
          tool: scope.tool.ident,
          data:
            service: { root: regionset.root }
            request: request
        scope.appendStep data: nextStep

