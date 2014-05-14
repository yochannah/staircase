define ['./options-dialogue'], (DialogueCtrl) -> Array '$scope', '$modal' , (scope, modals) ->

    scope.showOptions = ->
      dialogue = modals.open
        size: 'lg'
        templateUrl: '/partials/options-dialogue.html'
        controller: DialogueCtrl

