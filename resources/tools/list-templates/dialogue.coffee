define ['lodash', 'imjs', './template-controller'], (L, {Query}, TemplateController) ->

  isSuitableForType = (type, model) -> (template) ->
    query = new Query L.extend {}, template, {model}
    return false if not query.views.length
    [con, conN] = (c for c in query.constraints when c.editable)
    return false if (not con) or conN
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
