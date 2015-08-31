define ['imjs', 'underscore', 'jquery'], ({Service}, _, jquery) ->
  Array '$scope', 'Mines', 'Tables', '$document', (scope, Mines, Tables, $document) ->

    require.loadCss "/vendor/im-tables/dist/main.sandboxed.css"



    scope.getCollectionQuery = (collection) ->

      # query =
      #   from: scope.step.data.type
      #   select
      console.log "OUCH"
      # debugger;
      query =
        from: scope.step.data.type
        select: ["#{collection}.*"]
        where: [
          'path': 'id'
          'op': '='
          'value': '124467155'
        ]


    # obj.scrollTo


    # debugger;
    Tables.configure 'DefaultPageSize', '5'
    Tables.configure 'TableCell.IndicateOffHostLinks', false
    Tables.configure 'ShowHistory', false
    # debugger;


    scope.references = []

    debugger;

    Mines.get('humanmine')
         .then(Service.connect)
         .then (conn) ->
           console.log "Got item type", itemtype
           conn.makePath itemtype
           .then (pi) ->
             console.log "got pi", pi
             debugger;
             queryCollections pi, conn


    itemtype = scope.step.data.type
    scope.itemname = itemname = "1A03_HUMAN"

    queryCollections = (pi, conn) ->

      debugger;



      # Get our display name
      pi.getDisplayName().then (dn) -> scope.$apply -> scope.dn = dn

      # Build a query to get all of the items data attributes

      # attributeDescriptors = pi.allDescriptors()[0].attributes
      attributenames = (attribute for attribute of pi.allDescriptors()[0].attributes)
      scope.collections = (collection for collection of pi.allDescriptors()[0].collections)

      debugger;

      attributesquery =
        'from': "Gene"
        select: attributenames
        where: [
          'op': 'LOOKUP'
          'value': "test"
          'path': 'Protein'
        ]

      # attributesquery =
      #   'from': itemtype
      #   select: attributenames
      #   where: [
      #     'op': 'LOOKUP'
      #     'value': itemname
      #     'path': 'Protein'
      #   ]



      # Gets the items's attributes
      conn.tableRows(attributesquery).then (values) ->

        # Get the first result of the query
        if _.isArray values then values = values.pop()

        values = _.map values, (attr) ->
          attr.human = _.last attr.column.split(".")
          return attr

        scope.$apply -> scope.attributes = values
