# Karma configuration
# Generated on Wed Apr 16 2014 13:03:20 GMT+0100 (BST)

module.exports = (config) ->
  config.set

    # base path that will be used to resolve all patterns (eg. files, exclude)
    # The base path is relative to this file's location.
    basePath: '../'

    # frameworks to use
    # available frameworks: https://npmjs.org/browse/keyword/karma-adapter
    frameworks: ['jasmine', 'requirejs']

    # list of files / patterns to load in the browser/serve from karma.
    files: [
      {pattern: 'resources/public/vendor/**/*.js', included: false}, # Library code
      {pattern: 'src/coffee/**/*.coffee', included: false}, # Application code
      {pattern: 'resources/public/js/*.js', included: false} # Any js code.
      {pattern: 'test/steps/unit/*.coffee', included: false}, # Test code
      'test/steps/test-main.coffee' # Test entry-point
    ]

    # Exclude the application main file - we don't want to start the app, just run tests.
    exclude: [
      'src/coffee/init.coffee'
      'src/coffee/bootstrap.coffee'
    ]

    # preprocess matching files before serving them to the browser
    # available preprocessors: https://npmjs.org/browse/keyword/karma-preprocessor
    preprocessors: {
      '**/*.coffee': ['coffee']
    }

    # test results reporter to use
    # possible values: 'dots', 'progress'
    # available reporters: https://npmjs.org/browse/keyword/karma-reporter
    reporters: ['progress']

    # web server port
    port: 9876

    # enable / disable colors in the output (reporters and logs)
    colors: true

    client:
      mocha:
        ui: 'bdd'

    # level of logging
    # possible values:
    # - config.LOG_DISABLE
    # - config.LOG_ERROR
    # - config.LOG_WARN
    # - config.LOG_INFO
    # - config.LOG_DEBUG
    logLevel: config.LOG_INFO


    # enable / disable watching file and executing tests whenever any file changes
    autoWatch: true


    # start these browsers
    # available browser launchers: https://npmjs.org/browse/keyword/karma-launcher
    browsers: ['Firefox']


    # Continuous Integration mode
    # if true, Karma captures browsers, runs the tests and exits
    singleRun: false
