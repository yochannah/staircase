define ['lodash', 'imjs', './template-controller'], (L, {Query}, TemplateController) ->

  isSuitableForType = (type, model) -> (template) ->
    query = new Query L.extend {}, template, {model}
    return false if not query.views.length
    [con, conN] = editables = L.where(query.constraints, editable: true)
    return false if (not con)

    if conN # We can handle some classes of multiple constraints.
      lookups = L.where(editables, op: 'LOOKUP')
      if lookups.length isnt 1
        console.log "Template #{ template.name } isnt suitable because it doesn't have one lookup", template
        return false
      others = (c for c in editables when c.op isnt 'LOOKUP')
      unless L.all(others, (c) -> c.switchable)
        console.log  "Template #{ template.name } isnt suitable because the other editable are not switchable", template
        return false

    path = query.makePath con.path
    path = path.getParent() if path.isAttribute()
    path.isa type

  getParsedTitle = ({title, name}) -> (title or name).replace /.*--> /, ''

  inject = ['$scope', '$modalInstance', 'service', 'list', 'items', 'templates', 'model']

  controller = (scope, modal, service, list, items, templatesByName, model) ->

    scope.descLimit = 120 # characters. Tweet sized is best.
    scope.TemplateController = TemplateController
    scope.service = service
    scope.list = list
    scope.items = items

    isSuitable = isSuitableForType (list ? items).type, model

    templates = L.values templatesByName
    for template in templates
      template.parsedTitle = getParsedTitle template
    scope.templates = templates.filter isSuitable
    scope.cancel = -> modal.dismiss 'cancel'
    scope.run = (template) -> modal.close template

  return [inject..., controller]
