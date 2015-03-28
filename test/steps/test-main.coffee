allTestFiles = []
TEST_REGEXP = /spec.(coffee|js)$/i

pathToModule = (path) ->
  path.replace /^\/base\//,      ''
      .replace /\.(js|coffee)$/, ''

Object.keys(window.__karma__.files).forEach (file) ->
  # Normalize paths to RequireJS module names.
  return if /vendor/.test file # Make sure we don't load vendor tests
  allTestFiles.push file if TEST_REGEXP.test(file)
  return

require.config
  # Karma serves files under /base, which is the basePath from your config file
  baseUrl: '/base/src/coffee'

  paths:
    analytics:            '../../resources/public/js/argus'
    'angular-animate':    '../../resources/public/vendor/angular-animate/angular-animate'
    'angular-cookies':    '../../resources/public/vendor/angular-cookies/angular-cookies'
    'angular-local-storage': '../../resources/public/vendor/angular-local-storage/dist/angular-local-storage.min'
    'angular-mocks':      '../../resources/public/vendor/angular-mocks/angular-mocks'
    'angular-notify':     '../../resources/public/vendor/angular-notify/dist/angular-notify',
    'angular-resource':   '../../resources/public/vendor/angular-resource/angular-resource'
    'angular-route':      '../../resources/public/vendor/angular-route/angular-route'
    'angular-silent':     '../../resources/public/vendor/angular-silent/ngSilent'
    'angular-ui-select2': '../../resources/public/vendor/angular-ui-select2/src/select2'
    'angular-ui':         '../../resources/public/vendor/angular-ui-bootstrap-bower/ui-bootstrap-tpls'
    angular:              '../../resources/public/vendor/angular/angular'
    'angular-xeditable':  '../../resources/public/vendor/angular-xeditable/dist/js/xeditable.min'
    domReady:             '../../resources/public/vendor/requirejs-domready/domReady'
    font:                 '../../resources/public/vendor/requirejs-plugins/src/font'
    goog:                 '../../resources/public/vendor/requirejs-plugins/src/goog'
    headroom:             '../../resources/public/vendor/headroom.js/dist/headroom'
    image:                '../../resources/public/vendor/requirejs-plugins/src/image'
    imjs:                 '../../resources/public/vendor/imjs/js/im'
    jquery:               '../../resources/public/vendor/jquery/dist/jquery.min'
    jschannel:            '../../resources/public/vendor/jschannel'
    json:                 '../../resources/public/vendor/requirejs-plugins/src/json'
    markdownConverter:    '../../resources/public/vendor/requirejs-plugins/lib/Markdown.Converter'
    mdown:                '../../resources/public/vendor/requirejs-plugins/src/mdown.js'
    'ng-headroom':        '../../resources/public/vendor/headroom.js/dist/angular.headroom'
    pluralize:            '../../resources/public/vendor/pluralize/pluralize',
    select2:              '../../resources/public/vendor/select2/select2'
    text:                 '../../resources/public/vendor/requirejs-plugins/lib/text'
    underscore:           '../../resources/public/vendor/lodash/dist/lodash.underscore'
    # Test resources:
    angularMocks:         '../../resources/public/vendor/angular-mocks/angular-mocks'
    should:               '../../resources/public/vendor/should/should'
    jasmine:              '../../resources/public/vendor/jasmine/lib/jasmine-core'
    fixtures:             '../../steps/test/unit/fixtures'

  packages: [
    {
      name: 'lodash',
      main: 'lodash',
      location: '../../resources/public/vendor/lodash/dist'
    }
  ]

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
    'angular-notify':
      deps: ['angular']
    'angular-ui-select2':
      deps: ['angular', 'select2']
    'angular-mocks':
      deps: [ 'angular' ]
      exports: 'angular.mock'
    jschannel:
      exports: 'Channel'
    angularMocks:
      deps: ['angular']
      exports: 'angular.mock'

  # dynamically load all test files
  deps: allTestFiles

  callback: window.__karma__.start
