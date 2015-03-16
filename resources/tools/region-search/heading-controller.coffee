define ['imjs', 'lodash'], ({Service}, L) ->

  # We need to specify the taxon-id for each segment, otherwise the results _will_ be wrong.
  toFullRegions = (rows) -> ("#{chr}:#{s}..#{e}" for [chr, s, e, taxon] in rows)

  
  Array '$q', '$log', '$scope', 'connectTo', '$filter', 'ClassUtils', (Q, console, scope, connectTo, filter, ClassUtils) ->

    # :: RegionSet {root :: string,
    #               targetClass :: {name :: string, path :: Path},
    #               summary :: string, 
    #               fetch :: () -> Promise [[object]]}
    scope.regionset = {}
    # :: [RegionSet]
    scope.regionsets = []
    # :: [{name :: string, path :: Path }]
    scope.classes = []
    scope.enabled = true
    name = scope.data.name
    type = scope.data.type

    step = scope.previousStep
    scope.origin = (step.data.url or step.data.root or step.data.service.root)
    getService = connectTo(scope.origin)

    getModel = getService.then (service) -> service.fetchModel()
    getService.then (service) ->
      scope.serviceName = service.name
      scope.enabled = scope.enabled and ((not scope.tool.args) or (scope.tool.args.service is service.name))

    getClasses = getModel.then ClassUtils.getSubTypesOf 'SequenceFeature'

    Q.all([getService, getModel, getClasses]).then ([service, model, classes]) ->
      [targetClass] = scope.classes = classes.map (c) ->
        c.name = filter('pluralize')(c.name, 2)
        return c

      path = model.makePath(type)

      orgQ =
        from: type
        where: [[type, 'IN', name]]

      if path.isa('Location')
        orgQ.select = ['locatedOn.organism.shortName']
      else
        orgQ.select = ['organism.shortName']

      service.values(orgQ).then (names) ->
        scope.enabled = (scope.enabled and names.length is 1)
        scope.orgName = names[0]

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
          organism: scope.orgName
          types: [regionset.targetClass.path.toString()]
        nextStep =
          title: 'Search for overlapping features',
          tool: scope.tool.ident,
          data:
            service: { root: regionset.root }
            request: request
        scope.appendStep data: nextStep

