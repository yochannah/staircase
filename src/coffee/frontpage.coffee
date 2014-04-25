require.config
  baseUrl: '/js',
  shim:
    'angular':
      exports: 'angular'
    'angular-route': ['angular']
    'angular-ui': ['angular']
    'angular-mocks':
      deps: [ 'angular' ]
      exports: 'angular.mock'
    priority: [ 'angular' ]
  packages: [
    {
      name: 'lodash',
      location: '/vendor/lodash/dist',
      main: 'lodash'
    }
  ]
  paths:
    angular:         '/vendor/angular/angular'
    'angular-route': '/vendor/angular-route/angular-route'
    'angular-resource': '/vendor/angular-resource/angular-resource'
    'angular-mocks': '/vendor/angular-mocks/angular-mocks'
    'angular-cookies': '/vendor/angular-cookies/angular-cookies'
    'angular-ui':    '/vendor/angular-ui-bootstrap-bower/ui-bootstrap-tpls'
    text:            '/vendor/requirejs-text/text'
    imjs:            '/vendor/imjs/js/im'

# See http://code.angularjs.org/1.2.1/docs/guide/bootstrap#overview_deferred-bootstrap
window.name = 'NG_DEFER_BOOTSTRAP!'

deps = ['angular', 'services', 'controllers', 'directives', 'filters', 'app', 'routes']

# Have to require all modules here eagerly, as they must be available before resumeBootstrap
# is called.
require deps, (angular) ->
  $html = angular.element document.getElementsByTagName('html')[0]
  
  $html.ready -> angular.resumeBootstrap()
