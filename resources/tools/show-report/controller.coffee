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
    scope.showLoader = true

    path = conn.makePath scope.step.data.type

    path.then (pi) ->
      queryCollections pi, conn


    queryCollections = (pi, conn) ->

      # Get our display name
      pi.getDisplayName().then (dn) -> scope.$apply -> scope.dn = dn

      # Build a query to get all of the items data attributes

      attributenames = (attribute for attribute of pi.allDescriptors()[0].attributes)

      attributesquery =
        'from': scope.step.data.type
        select: attributenames
        where: [
          'path': 'id'
          'op': '='
          'value': scope.step.data.ids[0]
        ]

      # Gets the items's attributes
      conn.tableRows(attributesquery).then (values) ->
        scope.showLoader = false; #enough things show now we can hide loadeyimg

        # Get the first result of the query
        if _.isArray values then values = values.pop()
        scope.item = {}
        values = _.map values, (attr) ->

          #get the display name for the result.
          conn.makePath(attr.column).then (results) ->
            results.getDisplayName().then (dispName) ->
              attr.propName = _.last dispName.split(" > ");
              #update the scope with the pretty names when we get them
              values = valuesToObject values
              scope.$apply -> scope.attributes = values
              #show the not-pretty names until the promise above resolves.
          attr.propName = _.last attr.column.split(".")
          scope.item[attr.propName] = attr.value
          return attr

        values = valuesToObjects values
        scope.$apply -> scope.attributes = values

    #Converts to object. Easier to reference specific properties if needed.
    valuesToObject = (values) ->
      obj = _.object(_.map(values, (item) ->
        [
          item.propName
          item
        ]
      ))
      obj = setScopeDescription obj
      return obj

    #Snips out the description if present so we can show it up the top. Yay.
    setScopeDescription = (obj) ->
      description = obj.description || obj.Description
      if description
        console.log description
        scope.description = description
      delete obj.description
      return obj
