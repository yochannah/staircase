// we get all the test files automatically
var tests = [];
for (var file in window.__karma__.files) {
  if (window.__karma__.files.hasOwnProperty(file)) {
    if (/spec\.js$/i.test(file)) {
      tests.push(file);
    }
  }
}

require.config({
  paths: {
    angular:      '/base/resources/public/vendor/angular/angular',
    angularRoute: '/base/resources/public/vendor/angular-route/angular-route',
    angularMocks: '/base/resources/public/vendor/angular-mocks/angular-mocks',
    text:         '/base/resources/public/vendor/requirejs-text/text',
    fixtures:     '/base/test/unit/fixtures'

  },
  baseUrl: '/base/app/js',
  shim: {
    'angular' : {'exports' : 'angular'},
    'angularRoute': ['angular'],
    'angularMocks': {
      deps:['angular'],
      'exports':'angular.mock'
    }
  },
  deps: tests,
  callback: window.__karma__.start
});
