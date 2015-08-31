define ['imjs', 'underscore', 'jquery'], ({Service}, _, jquery) ->
  Array '$scope', 'Mines', 'Tables', '$document', (scope, Mines, Tables, $document) ->

    require.loadCss "/vendor/im-tables/dist/main.sandboxed.css"


    # Builds a query for each collection
    # belonging to the target item. This is used to populate
    # each collection table.
    scope.getCollectionQuery = (collection) ->
      query =
        from: scope.step.data.type
        select: ["#{collection}.*"]
        where: [
          'path': 'id'
          'op': '='
          'value': scope.step.data.id
        ]

    # Configure the tables to be a bit smaller
    Tables.configure 'DefaultPageSize', '5'
    Tables.configure 'TableCell.IndicateOffHostLinks', false
    Tables.configure 'ShowHistory', false



    scope.references = []

    # We need to rebuild our connection to intermine (look into this later)
    conn = new imjs.Service.connect scope.step.data.service

    scope.queryconn = "abc"


    path = conn.makePath scope.step.data.type

    #
    path.then (pi) ->
      queryCollections pi, conn


    #
    # itemtype = scope.step.data.type
    # scope.itemname = itemname = "1A03_HUMAN"

    queryCollections = (pi, conn) ->





      # Get our display name
      pi.getDisplayName().then (dn) -> scope.$apply -> scope.dn = dn

      # Build a query to get all of the items data attributes

      # attributeDescriptors = pi.allDescriptors()[0].attributes
      attributenames = (attribute for attribute of pi.allDescriptors()[0].attributes)
      scope.collections = (collection for collection of pi.allDescriptors()[0].collections)

      attributesquery =
        'from': scope.step.data.type
        select: attributenames
        where: [
          'path': 'id'
          'op': '='
          'value': scope.step.data.id
        ]


      # Gets the items's attributes
      conn.tableRows(attributesquery).then (values) ->

        # debugger;

        # Get the first result of the query
        if _.isArray values then values = values.pop()
        scope.item = {}
        values = _.map values, (attr) ->
          attr.human = _.last attr.column.split(".")
          scope.item[attr.human] = attr.value
          return attr





        scope.$apply -> scope.attributes = values
