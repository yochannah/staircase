require.loadCss = loadCss = (url) -> # Helper for css dependencies.
  link = document.createElement 'link'
  link.type = 'text/css'
  link.rel = 'stylesheet'
  link.href = url
  document.getElementsByTagName('head')[0].appendChild link

require.config
  baseUrl: '/js',

  shim:
    angular:
      exports: 'angular'
    underscore:
      exports: '_'
    'ng-headroom': ['angular', 'headroom']
    'angular-route': ['angular']
    'angular-animate': ['angular']
    'angular-ui': ['angular']
    'angular-cookies': ['angular']
    'angular-resource': ['angular']
    'angular-silent': ['angular']
    'angular-local-storage': ['angular']
    'angular-xeditable':
      deps: ['angular']
      init: -> loadCss '/vendor/angular-xeditable/dist/css/xeditable.css'
    'angular-notify':
      deps: ['angular']
      init: -> loadCss '/vendor/angular-notify/dist/angular-notify.css'
    'angular-ui-select2':
      deps: ['angular', 'select2']
      init: -> loadCss "/vendor/select2/select2.css"
    select2:
      deps: ['jquery']
    'angular-mocks':
      deps: [ 'angular' ]
      exports: 'angular.mock'
    jschannel: # TODO: replace with UMD fork.
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
    analytics:         '/js/argus' # named after the 100 eyed figure in el. myth.
    'angular-animate': '/vendor/angular-animate/angular-animate'
    'angular-cookies': '/vendor/angular-cookies/angular-cookies'
    'angular-local-storage': '/vendor/angular-local-storage/dist/angular-local-storage.min'
    'angular-mocks':   '/vendor/angular-mocks/angular-mocks'
    'angular-notify':  '/vendor/angular-notify/dist/angular-notify',
    'angular-resource': '/vendor/angular-resource/angular-resource'
    'angular-route':   '/vendor/angular-route/angular-route'
    'angular-silent':  '/vendor/angular-silent/ngSilent'
    'angular-ui-select2': '/vendor/angular-ui-select2/src/select2'
    'angular-ui':      '/vendor/angular-ui-bootstrap-bower/ui-bootstrap-tpls'
    angular:           '/vendor/angular/angular'
    'angular-xeditable': '/vendor/angular-xeditable/dist/js/xeditable.min'
    domReady:          '/vendor/requirejs-domready/domReady'
    font:              "/vendor/requirejs-plugins/src/font"
    goog:              "/vendor/requirejs-plugins/src/goog"
    headroom:          '/vendor/headroom.js/dist/headroom'
    image:             "/vendor/requirejs-plugins/src/image"
    imjs:              '/vendor/imjs/js/im'
    jquery:            '/vendor/jquery/dist/jquery.min' # Used for dataTransfer in drag-drop.
    jschannel:         '/vendor/jschannel'
    json:              "/vendor/requirejs-plugins/src/json"
    markdownConverter: "/vendor/requirejs-plugins/lib/Markdown.Converter"
    mdown:             "/vendor/requirejs-plugins/src/mdown.js"
    'ng-headroom':     '/vendor/headroom.js/dist/angular.headroom'
    pluralize:         '/vendor/pluralize/pluralize',
    select2:           '/vendor/select2/select2'
    text:              "/vendor/requirejs-plugins/lib/text"
    underscore:        '/vendor/lodash/dist/lodash.underscore'

  deps: ['./bootstrap']

