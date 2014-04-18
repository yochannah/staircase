define ['angular', 'services'], (ng) ->

  Services = ng.module('steps.services', [])

  Services.value('version', '0.1.0')

  Services.factory 'Persona', Array '$window', '$log', (win, log) ->
    watch = request = logout = -> log.warn "Persona authentication not available."
    win?.navigator?.id ? {watch, request, logout}

  return Services
