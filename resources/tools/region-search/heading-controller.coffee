define ['imjs', 'lodash'], ({Service}, L) -> Array '$scope', 'Mines', (scope, mines) ->

  scope.regionsets = []
  name = scope.data.name
  type = scope.data.type

  mines.atURL(scope.data.root or scope.service.root).then(Service.connect).then (service) ->
    service.fetchModel().then (model) -> # model aware type inspection
      scope.classes = (model.makePath(c) for c in model.getSubclassesOf('SequenceFeature'))
      path = model.makePath(type)
      if path.isa('Location')
        scope.regionsets.push
          root: service.root,
          targetClass: scope.classes[0]
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
          targetClass: scope.classes[0]
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

      scope.$apply()

  # We need to specify the taxon-id for each segment, otherwise the results _will_ be wrong.
  toFullRegions = (rows) -> ("#{taxon}:#{chr}:#{s}..#{e}" for [chr, s, e, taxon] in rows)
  
  scope.activate = (regionset) ->
    regionset.fetch().then (rows) ->
      request =
        regions: toFullRegions rows
        types: [regionset.targetClass.toString()]
      nextStep =
        title: 'Search for overlapping features',
        tool: scope.tool.ident,
        data:
          service: { root: regionset.root }
          request: request
      scope.appendStep data: nextStep

