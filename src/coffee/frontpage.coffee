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
  paths:
    angular:         '/vendor/angular/angular'
    'angular-route': '/vendor/angular-route/angular-route'
    'angular-mocks': '/vendor/angular-mocks/angular-mocks'
    'angular-cookies': '/vendor/angular-cookies/angular-cookies'
    'angular-ui':    '/vendor/angular-ui-bootstrap-bower/ui-bootstrap-tpls'
    text:            '/vendor/requirejs-text/text'

# See http://code.angularjs.org/1.2.1/docs/guide/bootstrap#overview_deferred-bootstrap
window.name = 'NG_DEFER_BOOTSTRAP!'

require ['angular', 'app'], (angular, app) ->
  $html = angular.element document.getElementsByTagName('html')[0]
  
  $html.ready -> angular.resumeBootstrap()
