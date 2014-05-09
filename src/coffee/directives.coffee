require ['angular', 'lodash', 'lines', 'jschannel', 'services'], (ng, L, lines, Channel) ->

  Directives = ng.module('steps.directives', ['steps.services'])

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

  Directives.directive 'currentStep', Array '$window', 'Mines', 'stepConfig', ($window, mines, conf) ->
    restrict: 'C'
    scope:
      tool:     '='
      step:     '='
      fullSize: '='
      onToggle: '&'
      hasItems: '&'
      hasList:  '&'
      nextStep: '&'
    template: """
      <div class="panel-heading">
        <i class="fa pull-right"
           ng-click="onToggle()"
           ng-class="{'fa-compress': fullSize, 'fa-expand': !fullSize}"></i>
        {{tool.heading}}
      </div>
      <div class="panel-body">
        <iframe is-seamless="tool.seamless" src="{{tool.src}}" width="100%">
      </div>
    """
    link: (scope, element, attrs) ->
      element.addClass('panel panel-default')
      iframe = element.find('iframe')

      do resize = -> iframe.css height: ng.element($window).height() * 0.85

      $window.addEventListener 'resize', resize

      channel = Channel.build
        window: iframe[0].contentWindow
        origin: '*'
        scope: 'CurrentStep'
        onReady: -> console.log "Channel ready"

      channel.bind 'next-step', (trans, data) -> scope.nextStep {data}

      channel.bind 'has-items', (trans, {key, noun, categories, ids}) ->
        scope.hasItems {type: noun, key, ids}

      channel.bind 'has-list', (trans, data) -> scope.hasList {data}

      for link in $window.document.getElementsByTagName('link') then do (link) ->
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

  Directives.directive 'startingPoint', ($window) ->
    restrict: 'E'
    controller: ($scope) ->
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
              panelBody.style.height = "#{ totalHeight - (p1.y - panelBody.offsetTop) }px"
              element.addClass 'expanded'

          $window.addEventListener 'resize', ->
            element[0].querySelector('.panel-body')?.style.height = null
            element.removeClass 'expanded'
            setTimeout doResize, 10

          setTimeout doResize, 10

  Directives.directive 'nativeTool', ($window, $compile, $injector) ->
    restrict: 'E'
    scope:
      tool: '=tool'
    link: (scope, element, attrs) ->

      scope.$watch 'tool.controllerURI', ->
        if scope.tool

          ctrl = $window.location.origin + scope.tool.controllerURI
          tmpl = $window.location.origin + scope.tool.templateURI

          require [ctrl, "text!#{ tmpl }"], (controller, template) ->

            $injector.invoke controller, this, {'$scope': scope}

            element.html(template)
            $compile(element.contents())(scope)

            scope.$apply()

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

          ctrl = $window.location.origin + scope.tool.headingControllerURI
          tmpl = $window.location.origin + scope.tool.headingTemplateURI

          require [ctrl, "text!#{ tmpl }"], (controller, template) ->

            $injector.invoke controller, this, {'$scope': scope}

            element.html(template)
            $compile(element.contents())(scope)

            scope.$apply()


