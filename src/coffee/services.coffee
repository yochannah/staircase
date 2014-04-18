define ['angular', 'services'], (ng) ->

  Services = ng.module('steps.services', [])

  Services.value('version', '0.1.0')

  Services.factory 'Persona', Array '$window', '$log', (win, log) ->
    dummyWatch = -> log.warn "Persona authentication not available."
    {watch: (win.navigator?.id?.watch ? dummyWatch)}

  return Services
