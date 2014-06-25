define ['lodash', 'imjs', './template-controller'], (L, {Query}, TemplateController) ->

  isSuitable = (list, model) -> (template) ->
    query = new Query L.extend {}, template, {model}
    [con, conN] = (c for c in query.constraints when c.editable)
    return false if (not con) or conN
    path = query.makePath con.path
    path = path.getParent() if path.isAttribute()
    path.isa list.type

  getParsedTitle = ({title, name}) -> (title or name).replace /.*--> /, ''

  controller = (scope, modal, list, byName, model) ->

    scope.descLimit = 120 # characters. Tweet sized is best.
    scope.TemplateController = TemplateController
    scope.service = list.service
    scope.list = list

    templates = L.values byName
    for template in templates
      template.parsedTitle = getParsedTitle template
    scope.templates = templates.filter isSuitable list, model
    scope.cancel = -> modal.dismiss 'cancel'
    scope.run = (template) -> modal.close template
  
  Array '$scope', '$modalInstance', 'list', 'templates', 'model', controller
