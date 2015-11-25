(ns staircase.views.about
  (:use [hiccup.core :only (html)]
        [hiccup.element :only (image link-to)])
  (:require [staircase.views.forms :refer (search-input)]))

(defn about-header [config]
  [:div.about-header
   [:div.container
    [:div.row
     [:div.col-md-8.col-md-offset-2
      [:h1 {:style "white-space: nowrap"}
       (image "/images/flattened-helix-right-128px.png")
       (:project-title config)]]]
    [:div.row
     [:form.search-form.col-sm-6.col-sm-offset-3 (search-input config)]]
    [:div.row
     [:p {:antiscroll "autoHide:false"} "The data-flow interface to InterMine data-warehouses,
         providing an extensible, programmable work-bench for
         scientists."]]]])

(defn snippet [config]
  (html [:div.container-fluid
   [:div.row (about-header config)]
   [:div.row
    [:div.about.col-xs-10.col-xs-offset-1

     [:section
      [:h2 "Using the home-page"]
      [:p.lead
       "The " (link-to "/" "home page") " allows quick access to the tools available "
       "for starting a new history"]
      [:p
       "Tools are components that let you interact with your data. They can do a wide
       range of things, from uploading a data-set, to running a complex query, to
       managing your stored resources. On the home page you can see the tools that can
       be used to start a sequence of steps in a data-flow. One of these is shown below:"]
      [:div.row
       [:starting-point.col-sm-6 ;; TODO: Move the template below into a directive. Currently not very DRY at all.
        [:div.panel.panel-default.first-step {:ng-class "tool.action ? 'with-action' : ''"}
         [:div.panel-heading
          [:i.fa.fa-undo.pull-right {:ng-show "tool.resettable" :ng-click "reset()"}]
          "{{tool.heading}}"]
         [:div.panel-body
          [:native-tool {:tool "tool"}]]
         [:div.panel-footer {:ng-if "tool.action"}
          [:button.btn.btn-default.pull-right {:ng-disabled "tool.disabled" :ng-click "act()"} "{{tool.action}}"]
          [:div.clearfix]]]]
       [:div.col-sm-6
        [:p {:ng-repeat "para in tool.description"} "{{para}}"]
        [:p
         "The following standard controls are available for interacting with one of these
         tools:"]
        [:table.table
          [:tr
           [:td
            [:button.btn.btn-default {:ng-disabled "tool.disabled" :ng-click "act()"} "{{tool.action}}"]]
           [:td
            "This button performs the main action for the component (if it has one)"]]
         [:tr
          [:td
            [:i.fa.fa-undo {:ng-click "reset()"}]]
          [:td
           "This button restores the component to its orginal state, removing any filters you may have applied."]]]]
        ]]]]]))
