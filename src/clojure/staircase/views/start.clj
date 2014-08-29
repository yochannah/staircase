(ns staircase.views.start
  (:require staircase.views.welcome))

(defn initiator
  [attrs panel-attrs heading-buttons]
  [:starting-point.xs-col-12 (assoc attrs :ng-hide "tool.error")
   [:div.panel.panel-default.first-step
    panel-attrs
    (apply vector :div.panel-heading
     (concat heading-buttons
             [ [:i.fa.fa-undo.pull-right {:ng-show "tool.resettable" :ng-click "reset()"}]
               "{{tool.heading}}" ]))
    [:div.panel-body
     [:native-tool {:tool "tool" :actions "buttons" :state "state"}]]
    [:div.panel-footer {:ng-if "tool.action"}
     [:button.btn.btn-default.pull-right
      {:ng-disabled "state.disabled" :ng-click "act()"}
      "{{buttons.act || tool.action}}"]
     [:div.clearfix]]]])

(defn starting-point [config]
  [:div.container-fluid

   [:div.row
    [:div.alert.alert-warning {:ng-show "tool.error"}
     [:p
      [:strong "Error"]
      " {{tool.error.message}}"]]
    (initiator {}
               {:class "full-height" :ng-class "{'with-action': tool.action}"}
               [])]])

(defn starting-points [config]

  [:div.container-fluid

   (staircase.views.welcome/snippet config)

   [:div.row.starting-points {:ng-controller "StartingPointsController"}

    [:div.col-sm-4.col-md-3.starting-headline
     {:ng-repeat "tool in startingPoints | filter:{active:true}"}
     [:a.headline-heading {:href "/starting-point/{{tool.ident}}/{{tool.args.service}}"} 
      [:div.pull-left.headline-icon [:i.fa.fa-fw.fa-4x {:ng-class "tool.icon"}]]
      [:h3 "{{tool.heading}}"]]
     [:starting-headline {:tool "tool"}]]]])

