define ['lodash'], (L) -> Array '$scope', '$log', '$modal', (scope, log, modalFactory) ->

  InputEditCtrl = Array '$scope', '$modalInstance', 'history', 'step', (scope, modal, history, step, index) ->

    scope.data = L.clone step.data
    delete scope.data.service # Not editable

    scope.ok = -> modal.close history.id, index, scope.data

    scope.cancel = -> modal.dismiss 'cancel'

  scope.openEditDialogue = ->
    dialogue = modalFactory.open
      templateUrl: '/partials/edit-step-data.html'
      controller: InputEditCtrl
      size: 'lg'
      resolve:
        index: -> scope.$index
        history: -> scope.history
        step: -> scope.s
