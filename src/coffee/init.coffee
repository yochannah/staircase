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
    'ng-headroom': ['angular', 'headroom']
    'angular-route': ['angular']
    'angular-animate': ['angular']
    'angular-ui': ['angular']
    'angular-cookies': ['angular']
    'angular-resource': ['angular']
    'angular-silent': ['angular']
    'angular-local-storage': ['angular']
    'angular-notify':
      deps: ['angular']
      init: -> loadCss '/vendor/angular-notify/dist/angular-notify.css'
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
    
    font: "/vendor/requirejs-plugins/src/font"
    goog: "/vendor/requirejs-plugins/src/goog"
    image: "/vendor/requirejs-plugins/src/image"
    json: "/vendor/requirejs-plugins/src/json"
    mdown: "/vendor/requirejs-plugins/src/mdown.js"
    text: "/vendor/requirejs-plugins/lib/text"
    markdownConverter: "/vendor/requirejs-plugins/lib/Markdown.Converter"
    analytics:       '/js/ga'
    angular:         '/vendor/angular/angular'
    headroom:        '/vendor/headroom.js/dist/headroom'
    'ng-headroom':   '/vendor/headroom.js/dist/angular.headroom'
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
    'angular-silent': '/vendor/angular-silent/ngSilent'
    'angular-xeditable': '/vendor/angular-xeditable/dist/js/xeditable.min'
    'angular-notify': '/vendor/angular-notify/dist/angular-notify',
    'pluralize': '/vendor/pluralize/pluralize',
    'angular-local-storage':
      '/vendor/angular-local-storage/dist/angular-local-storage.min'
    select2:         '/vendor/select2/select2'
    imjs:            '/vendor/imjs/js/im'
  deps: ['./bootstrap']

