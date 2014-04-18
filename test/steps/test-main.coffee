allTestFiles = []
TEST_REGEXP = /spec.(coffee|js)$/i
pathToModule = (path) ->
  path.replace(/^\/base\//, "").replace(/\.js$/, "").replace(/\.coffee$/, "")

Object.keys(window.__karma__.files).forEach (file) ->
  # Normalize paths to RequireJS module names.
  allTestFiles.push file if TEST_REGEXP.test(file)
  return

require.config
  paths:
    angular:      '/base/resources/public/vendor/angular/angular'
    angularRoute: '/base/resources/public/vendor/angular-route/angular-route'
    angularMocks: '/base/resources/public/vendor/angular-mocks/angular-mocks'
    text:         '/base/resources/public/vendor/requirejs-text/text'
    should:       '/base/resources/public/vendor/should/should'
    fixtures:     '/base/test/unit/fixtures'

  shim:
    angular: {exports: 'angular'}
    angularRoute: ['angular']
    angularMocks:
      deps: ['angular']
      exports: 'angular.mock'

  # Karma serves files under /base, which is the basePath from your config file
  baseUrl: "/base/src/coffee"

  # dynamically load all test files
  deps: allTestFiles

  callback: window.__karma__.start
