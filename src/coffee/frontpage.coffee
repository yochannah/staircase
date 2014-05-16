require.loadCss = loadCss = (url) -> # Helper for css dependencies.
  link = document.createElement 'link'
  link.type = 'text/css'
  link.rel = 'stylesheet'
  link.href = url
  document.getElementsByTagName('head')[0].appendChild link

require.config
  baseUrl: '/js',
  shim:
    'angular':
      exports: 'angular'
    'underscore':
      exports: '_'
    'angular-route': ['angular']
    'angular-animate': ['angular']
    'angular-ui': ['angular']
    'angular-cookies': ['angular']
    'angular-resource': ['angular']
    'angular-ui-select2':
      deps: ['angular', 'select2']
      init: -> loadCss "/vendor/select2/select2.css"
    'angular-mocks':
      deps: [ 'angular' ]
      exports: 'angular.mock'
    'jschannel':
      exports: 'Channel'
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
    domReady:        '/vendor/requirejs-domready/domReady'
    'underscore':    '/vendor/lodash/dist/lodash.underscore'
    'jschannel':      '/vendor/jschannel'
    'angular-route': '/vendor/angular-route/angular-route'
    'angular-animate': '/vendor/angular-animate/angular-animate'
    'angular-resource': '/vendor/angular-resource/angular-resource'
    'angular-mocks': '/vendor/angular-mocks/angular-mocks'
    'angular-cookies': '/vendor/angular-cookies/angular-cookies'
    'angular-ui':    '/vendor/angular-ui-bootstrap-bower/ui-bootstrap-tpls'
    'angular-ui-select2': '/vendor/angular-ui-select2/src/select2'
    select2:         '/vendor/select2/select2'
    text:            '/vendor/requirejs-text/text'
    imjs:            '/vendor/imjs/js/im'
  deps: ['./bootstrap']

# See http://code.angularjs.org/1.2.1/docs/guide/bootstrap#overview_deferred-bootstrap
# window.name = 'NG_DEFER_BOOTSTRAP!'

# deps = ['angular', 'angular-cookies', 'angular-route', 'angular-resource',
#        'services', 'controllers', 'directives', 'filters', 'app', 'routes']

# Have to require all modules here eagerly, as they must be available before resumeBootstrap
# is called.
# define deps, (angular) ->
# $html = angular.element document.getElementsByTagName('html')[0]
# 
# $html.ready -> angular.resumeBootstrap()
