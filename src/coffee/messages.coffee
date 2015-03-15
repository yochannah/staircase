define (require, exports, module) ->

  L = require 'lodash'

  module.exports = class Messages

    constructor: ->
      @_msgs = {}

    set: (key, msg) ->
      if L.isString msg
        msg = L.template msg
      @_msgs[key] = msg

    get: (key, data) ->
      if fn = @_msgs[key]
        fn data
      else
        "!! no message for #{ key } !!"

