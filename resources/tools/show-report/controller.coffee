define ['imjs', 'underscore', 'jquery'], ({Service}, _, jquery) ->
  Array '$scope', 'Mines', 'Tables', '$document', (scope, Mines, Tables, $document) ->

    require.loadCss "/vendor/im-tables/dist/main.sandboxed.css"


    # Builds a query for each collection
    # belonging to the target item. This is used to populate
    # each collection table.
    scope.getCollectionQuery = (collection) ->

    scope.references = []

    # We need to rebuild our connection to intermine (look into this later)
    conn = new imjs.Service.connect scope.step.data.service

    scope.queryconn = "abc"

    path = conn.makePath scope.step.data.type


    path.then (pi) ->
      queryCollections pi, conn


    queryCollections = (pi, conn) ->

      # Get our display name
      pi.getDisplayName().then (dn) -> scope.$apply -> scope.dn = dn

      # Build a query to get all of the items data attributes

      # attributeDescriptors = pi.allDescriptors()[0].attributes
      attributenames = (attribute for attribute of pi.allDescriptors()[0].attributes)

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
          console.log(attr.column, attr.column.split("."));
          attr.propName = _.last attr.column.split(".")
          scope.item[attr.propName] = attr.value
          return attr

        scope.$apply -> scope.attributes = values
