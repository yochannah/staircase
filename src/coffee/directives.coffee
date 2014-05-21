require ['angular', 'lodash', 'lines', 'jschannel', 'services'], (ng, L, lines, Channel) ->

  Directives = ng.module('steps.directives', ['steps.services'])

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

  Directives.directive 'iframeTool', Array '$window', 'Mines', 'stepConfig', (win, mines, conf) ->
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

      channel = Channel.build
        window: iframe[0].contentWindow
        origin: '*'
        scope: 'CurrentStep'
        onReady: -> console.log "Channel ready"

      channel.bind 'next-step', (trans, data) -> scope.nextStep {data}

      # TODO: don't make use register a handler for each type...
      channel.bind 'has-items', (trans, {key, noun, categories, ids}) ->
        scope.hasItems {type: noun, key, ids}

      channel.bind 'has-list', (trans, data) -> scope.has {what: 'list', data}

      channel.bind 'has-ids', (trans, data) -> scope.has {what: 'ids', data}

      channel.bind 'has', (trans, {what, data}) -> scope.has {what, data}

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

      scope.$watch 'step', (step) ->
        return unless step?
        step.$promise.then ->

          init = ->
            console.log "Initialising with", step.data
            channel.call
              method: 'init'
              params: step.data
              error: -> console.error "initialization failed"
              success: -> console.log "Initialized"

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

  Directives.directive 'currentStep', Array '$compile', ($compile) ->
    restrict: 'C'
    scope:
      tool:     '='
      step:     '='
      fullSize: '='
      onToggle: '&'
      hasItems: '&'
      has:      '&'
      wants:    '&'
      nextStep: '&'
    template: """
      <div class="panel-heading">
        <i class="fa pull-right"
           ng-click="onToggle()"
           ng-class="{'fa-compress': fullSize, 'fa-expand': !fullSize}"></i>
        {{tool.heading}}
      </div>
    """
    link: (scope, element, attrs) ->
      element.addClass('panel panel-default')
      scope.$watch 'tool.type', (toolType) ->
        console.log toolType

        toAppend = if toolType is 'IFrame'
          "<iframe-tool/>"
        else if toolType is 'native'
          "<angular-tool/>"

        if toAppend?
          $compile(toAppend) scope, (cloned) -> element.append cloned

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
            margin = 38 # HARDCODED! TODO! CALCULATE!
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
              totalHeight = ystep * factor - margin
              console.log totalHeight, ystep, factor
              panelBody.style.height = "#{ totalHeight - (p1.y - panelBody.offsetTop) }px"
              element.addClass 'expanded'

          $window.addEventListener 'resize', ->
            element[0].querySelector('.panel-body')?.style.height = null
            element.removeClass 'expanded'
            $timeout doResize, 10

          $timeout doResize, 10

  toolCssLoaded = {}

  injectingLinker = (window, compile, injector) -> (scope, element, attrs) ->
    scope.$watch 'tool.controllerURI', ->
      if scope.tool

        if scope.tool.style and not toolCssLoaded[scope.tool.style]
          link = window.document.createElement('link')
          link.rel = 'stylesheet'
          link.href = scope.tool.style
          window.document.getElementsByTagName('head')[0].appendChild(link)
          toolCssLoaded[scope.tool.style] = true

        ctrl = '.' + scope.tool.controllerURI
        tmpl = scope.tool.templateURI

        require {baseUrl: '/'}, [ctrl, "text!#{ tmpl }"], (controller, template) ->

          injector.invoke controller, this, {'$scope': scope}

          element.html(template)
          compile(element.contents())(scope)

          scope.$apply()

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

  Directives.directive 'nextStep', ($window, $compile, $injector) ->
    restrict: 'E'
    replace: true
    scope:
      previousStep: '='
      appendStep: '&'
      data: '='
      tool: '='
    link: (scope, element, attrs) ->

      scope.$watch 'tool.headingURI', ->
        if scope.tool

          ctrl = '.' + scope.tool.headingControllerURI
          tmpl = $window.location.origin + scope.tool.headingTemplateURI

          require {baseUrl: '/'}, [ctrl, "text!#{ tmpl }"], (controller, template) ->

            $injector.invoke controller, this, {'$scope': scope}

            element.html(template)
            $compile(element.contents())(scope)

            scope.$apply()


