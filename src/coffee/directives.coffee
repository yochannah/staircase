require ['angular', 'lodash', 'lines', 'jschannel', 'services'], (ng, L, lines, Channel) ->

  Directives = ng.module('steps.directives', ['steps.services'])

  Directives.directive 'focusMe', ($timeout, $parse) -> link: (scope, el, attrs) ->
    model = $parse attrs.focusMe
    scope.$watch model, (value) -> if value
      $timeout -> el[0].focus()
    el.bind 'blur', -> scope.$apply model.assign scope, false

  Directives.directive 'blurOn', ->
    restrict: 'A'
    scope:
      blurOn: '@'
    link: (scope, element) ->
      el = element[0]
      element.on scope.blurOn, el.blur.bind(el)

  Directives.directive 'typeName', ($q) ->
    restrict: 'E'
    scope:
      service: '='
      type: '='
      count: '=?'
    template: """
      <span class="type-name">
        <ng-pluralize count="count"
                      when="{'one': '{{displayName}}', 'other': '{{displayName}}s'}"/>
      </span>
    """
    link: (scope) -> scope.$watch 'service', (service) ->
      if !scope.count
        scope.count = 0
      if scope.type
        scope.displayName = scope.type
      return unless service?
      service.fetchModel().then (model) ->
        $q.when(model.makePath(scope.type).getDisplayName()).then (name) ->
          scope.displayName = name


  # Pair of recursive directives for turning any json structure into an editable form.
  Directives.directive 'editableItem', ($compile) ->
    restrict: 'E'
    replace: true
    scope:
      item: '='
    controller: ($scope) ->
      $scope.removeElementAt = (i) -> $scope.item.value.splice(i, 1)
    template: """
      <div class="form-group">
        <label>
          {{item.category == 'array' ? item.value.length : ''}} {{item.name}}
        </label>
      </div>
    """
    link: (scope, element) ->
      toAppend = if scope.item.category is 'bool'
        """
          <button class="btn btn-default boolean-control"
                  ng-class="{active: item.value}"
                  ng-click="item.value = !item.value">
            {{item.value}}
          </button>
        """
      else if scope.item.category is 'array'
        if L.isObject scope.item.value[0] # array of objects
          """
            <div class="form-group collapsible"
                 ng-class="{collapsed: elem.collapsed}"
                 ng-repeat="elem in item.value">
              <button class="pull-right btn btn-default"
                      ng-click="removeElem(item, elem)">
                Remove
              </button>
              <label ng-click="elem.collapsed = !elem.collapsed">
                <i class="fa"
                   ng-class="{'fa-caret-right': elem.collapsed, 'fa-caret-down': !elem.collapsed}">
                </i>
                {{item.name}} {{$index + 1}}
              </label>
              <editable-data data="elem"/>
            </div>
          """
        else # Array of scalar values
          """
            <div class="well well-sm clearfix">
              <span class="array-element label label-default"
                    ng-repeat="elem in item.value track by $index">
                <i tooltip="remove this element"
                   ng-click="removeElementAt($index)"
                   class="pull-left fa fa-times-circle"></i>
                <i tooltip="edit this element" class="pull-right fa fa-edit"></i>
                {{elem}}
              </span>
            </div>
          """
      else if scope.item.category is 'scalar'
        """<input class="form-control" ng-model="item.value">"""
      else if scope.item.category is 'object'
        """<editable-data data="item.value"/>"""

      $compile(toAppend) scope, (cloned) -> element.append cloned

  Directives.directive 'editableData', ->
    restrict: 'E'
    replace: true
    scope:
      data: '='
    controller: ($scope) ->
      categorise = (value) ->
        if L.isBoolean(value)
          'bool'
        else if L.isArray value
          'array'
        else if L.isObject value
          'object'
        else
          'scalar'

      $scope.$watch 'data', (data) ->
        $scope.items = for name, value of data when name[0] isnt '$'
          {name, value, category: categorise(value)}

    template: """
      <editable-item item="item" ng-repeat="item in items">
      </editable-item>
    """

  Directives.directive 'html5FileUpload', ->
    restrict: 'E'
    require: ['ngModel']
    replace: true
    template: """<input type="file">"""
    link: (scope, elem, attrs, [ngModel]) ->
      elem.on 'change', -> scope.$apply ->
        file = elem[0].files[0]
        ngModel.$setViewValue file

  Directives.directive 'appVersion', ['version', (v) -> (scope, elm) -> elm.text(version)]

  getOffsets = (doc, sel) ->
    els = [].slice.call doc.querySelectorAll sel
    for e in els
      left: e.offsetLeft
      top: e.offsetTop
      width: e.offsetWidth
      height: e.offsetHeight

  getTopBars = (offsets) ->
    for o in offsets
      p:
        x: o.left
        y: o.top
      q:
        x: o.left + o.width
        y: o.top

  doesntIntersectWith = (p1, q1) -> ({p, q}) -> not lines.intersects p1, q1, p, q

  # Handle the seamless attribute on iframes.
  Directives.directive 'isSeamless', ->
    restrict: 'A'
    scope:
      isSeamless: '='
    link: (scope, element, attrs) ->
      scope.$watch 'isSeamless', (seamless) ->
        if seamless
          element.attr 'seamless', 'seamless'
        else
          element.removeAttr('seamless')

  Directives.directive 'iframeTool', Array '$log', '$window', 'Mines', 'stepConfig', (console, win, mines, conf) ->
    restrict: 'E'
    replace: true
    template: """
      <div class="panel-body">
        <iframe is-seamless="tool.seamless" src="{{tool.src}}" width="100%">
      </div>
    """
    link: (scope, element, attrs) ->
      iframe = element.find('iframe')

      do resize = -> iframe.css height: ng.element(win).height() * 0.85

      win.addEventListener 'resize', resize

      console.debug 'connecting to iframe'

      channel = Channel.build
        window: iframe[0].contentWindow
        origin: '*'
        scope: 'CurrentStep'
        onReady: -> console.log "Channel ready"

      console.debug 'binding to channel'
      channel.bind 'next-step', (trans, step) -> scope.nextStep data: step

      channel.bind 'change-state', (trans, step) ->
        step.tool = scope.tool.ident # this message can only be handled by the same tool
        console.log 'shhh', step.title, step
        scope.silently data: step

      # TODO: don't make users register a handler for each type...
      channel.bind 'has-items', (trans, {key, noun, categories, ids}) ->
        scope.hasItems {type: noun, key, ids}

      channel.bind 'has-list', (trans, data) -> scope.has {what: 'list', data}

      channel.bind 'has-ids', (trans, data) -> scope.has {what: 'ids', data}

      # params should be an object of form {what, data}
      channel.bind 'has', (trans, params) -> scope.has params

      channel.bind 'wants', (trans, {what, data}) -> scope.wants {what, data}

      for link in win.document.getElementsByTagName('link') then do (link) ->
        channel.call
          method: 'style'
          params:
            stylesheet: link.href
          success: -> console.log "Applied stylesheet: #{ link.href }"

      scope.$watch 'tool.ident', (ident) ->
        return unless ident and conf[ident]
        channel.call
          method: 'configure'
          params: conf[ident]
          success: -> console.log "Configured #{ ident }"

      initialised = false
      scope.$watch 'step', (step) ->
        return if initialised or not step?
        step.$promise.then ->

          init = ->
            console.log "Initialising with", step.data
            channel.call
              method: 'init'
              params: step.data
              error: -> console.error "initialization failed"
              success: ->
                console.log "Initialized"
                initialised = true

          if step.data.service?.root and not step.data.service.token
            root = step.data.service.root
            console.log "NEEDS A TOKEN FOR", root
            mines.all().then (services) ->
              console.log "SERVICES", services.map (s) -> "#{ s.root } => #{ s.token }"
              token = do (services) ->
                for s in services when root.indexOf(s.root) >= 0
                  return s.token
              console.log "TOKEN", token
          
              step.data.service.token = token
              init()
          else
            init()

  interpolatedHeading = "<span>{{tool.heading}}</span>"

  managedHeading = (src) -> """<ng-include src="'#{ src }'"></ng-include>"""

  Directives.directive 'currentStep', Array '$compile', ($compile) ->
    restrict: 'C'
    scope:
      tool:     '='
      step:     '='
      state:    '='
      toggle:   '&'
      hasItems: '&'
      has:      '&'
      wants:    '&'
      nextStep: '&'
      silently: '&'
    template: """
      <div class="panel-heading">
        <i class="fa pull-right"
           ng-click="toggle()"
           ng-class="{'fa-compress': state.expanded, 'fa-expand': !state.expanded}"></i>
      </div>
    """
    link: (scope, element, attrs) ->
      element.addClass('panel panel-default')
      console.log 'currentStep.link called'
      compiled = false
      scope.$watch 'tool.type', (toolType) ->
        return if compiled or not toolType?

        heading = if scope.tool.panelHeading?
          managedHeading scope.tool.panelHeading
        else
          interpolatedHeading

        console.log 'Compiling current step'

        body = if toolType is 'IFrame'
          "<iframe-tool/>"
        else if toolType is 'native'
          "<angular-tool/>"
        else
          """<div class="alert alert-warning">
              Configuration error: unknown tool type - #{ toolType }
            </div>
          """

        $compile(heading) scope, (cloned) -> element.children()[0].appendChild cloned[0]

        $compile(body) scope, (cloned) ->
          element.append cloned
          compiled = true


  Directives.directive 'filterTerm', ->
    restrict: 'AE'
    replace: true,
    scope:
      term: '='
    template: """
      <div class="input-group input-group-sm">
          <span class="input-group-addon">Filter:</span>
          <input class="form-control" ng-model="term">
          <span class="input-group-btn">
              <button class="btn btn-default" ng-click="term = null">
                <i class="fa fa-times"></i>
              </button>
          </span>
      </div>
    """

  Directives.directive 'startingPoint', ($timeout, $window) ->
    restrict: 'E'
    controller: ($scope) ->
      $scope.buttons = {}
      $scope.state = {}
      $scope.reset = -> $scope.$broadcast 'reset'
      $scope.act = -> $scope.$broadcast 'act'

    link: (scope, element, attrs) ->

      scope.$watch 'tool.expandable', (expandable) ->
        if expandable
          doResize = ->
            margin = 30 # HARDCODED! TODO! CALCULATE!
            offsets = getOffsets(document, 'starting-point')
            maxx = L.max offsets.map (o) -> o.left + o.width
            maxy = L.max offsets.map (o) -> o.top + o.height
            topBars = getTopBars offsets
            ystep = element[0].offsetHeight
            unless ystep
              $timeout doResize, 10
              return

            p1 =
              x: element[0].offsetLeft
              y: element[0].offsetTop + 1 # Don't get intersects with own top bar.
            q1 =
              x: p1.x + element[0].offsetWidth
              y: p1.y + element[0].offsetHeight

            factor = 1

            while factor < 3 and (q1.y + ystep) <= maxy
              q1.y += ystep + margin
              if L.every topBars, doesntIntersectWith p1, q1
                factor += 1

            if factor > 1 and panelBody = element[0].querySelector('.panel-body')
              totalHeight = ystep * factor - ((factor - 1) * margin)
              console.log totalHeight, ystep, factor
              panelBody.style.height = "#{ totalHeight - (p1.y - panelBody.offsetTop) }px"
              element.addClass 'expanded'

          $window.addEventListener 'resize', ->
            element[0].querySelector('.panel-body')?.style.height = null
            element.removeClass 'expanded'
            $timeout doResize, 10

          $timeout doResize, 10

  toolCssLoaded = {}

  loadStyle = (window, {ident, style}) ->
    if style and not toolCssLoaded[ident]
      link = window.document.createElement('link')
      link.rel = 'stylesheet'
      link.href = style
      window.document.getElementsByTagName('head')[0].appendChild(link)
      toolCssLoaded[ident] = true

  injectingLinker = (window, compile, injector, curi = 'controllerURI', turi = 'templateURI') ->
    (scope, element, attrs) ->
      scope.$watch "tool.#{ curi }", (ctrl) -> if ctrl
        loadStyle window, scope.tool

        tmpl = scope.tool[turi]
        console.log ctrl, tmpl

        require {baseUrl: '/'}, ['.' + ctrl, "text!#{ tmpl }"], (controller, template) ->

          c = injector.instantiate controller, {'$scope': scope}
          compileScope = scope.$new false
          compileScope.controller = c

          element.html(template)
          compile(element.contents())(compileScope)

          scope.$apply()

  Directives.directive 'startingHeadline', ($window, $compile, $injector) ->
    restrict: 'E'
    scope:
      tool: '=tool'
    link: injectingLinker $window, $compile, $injector, 'headlineControllerURI', 'headlineTemplateURI'

  Directives.directive 'nativeTool', ($window, $compile, $injector) ->
    restrict: 'E'
    scope:
      tool: '=tool'
      actions: '=actions'
      state: '=state'
    link: injectingLinker $window, $compile, $injector

  Directives.directive 'angularTool', ($window, $compile, $injector) ->
    restrict: 'E'
    link: injectingLinker $window, $compile, $injector
    controller: ($scope) -> $scope.$on 'has', (event, msg) -> $scope.has msg # {what, key, data}

  Directives.directive 'nextStep', ($window, $compile, $injector) ->
    restrict: 'E'
    replace: true
    scope:
      previousStep: '='
      appendStep: '&'
      data: '='
      tool: '='
      service: '=?'
    link: (scope, element, attrs) ->

      scope.$watch 'tool.headingURI', ->

        if scope.tool

          loadStyle $window, scope.tool

          ctrl = '.' + scope.tool.headingControllerURI
          tmpl = $window.location.origin + scope.tool.headingTemplateURI

          require {baseUrl: '/'}, [ctrl, "text!#{ tmpl }"], (controller, template) ->
            scope.previousStep.$promise.then -> # Resolve the previous step.

              $injector.invoke controller, this, {'$scope': scope}

              element.html(template)
              $compile(element.contents())(scope)

              # scope.$apply()

