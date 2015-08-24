define ['imjs', 'underscore', 'jquery', 'angular-scroll'], ({Service}, _, jquery) ->
  Array '$scope', 'Mines', 'Tables', '$document', (scope, Mines, Tables, $document) ->

    require.loadCss "/vendor/im-tables/dist/main.sandboxed.css"


    scope.getCollectionQuery = (collection) ->
      query =
        from: itemtype
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

    Mines.get('humanmine')
         .then(Service.connect)
         .then (conn) ->
           conn.makePath itemtype
           .then (pi) ->
             queryCollections pi, conn


    itemtype = "Protein"
    scope.itemname = itemname = "1A03_HUMAN"

    queryCollections = (pi, conn) ->

      # Get our display name
      pi.getDisplayName().then (dn) -> scope.$apply -> scope.dn = dn

      # Build a query to get all of the items data attributes

      # attributeDescriptors = pi.allDescriptors()[0].attributes
      attributenames = (attribute for attribute of pi.allDescriptors()[0].attributes)
      scope.collections = (collection for collection of pi.allDescriptors()[0].collections)


      attributesquery =
        'from': itemtype
        select: attributenames
        where: [
          'op': 'LOOKUP'
          'value': itemname
          'path': 'Protein'
        ]



      # Gets the items's attributes
      conn.tableRows(attributesquery).then (values) ->

        # Get the first result of the query
        if _.isArray values then values = values.pop()

        values = _.map values, (attr) ->
          attr.human = _.last attr.column.split(".")
          return attr

        scope.$apply -> scope.attributes = values
