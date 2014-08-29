(ns staircase.views.facets
  (:require [hiccup.def :refer (defelem)]))

(defelem snippet []
  [:div.facets.panel.panel-default
   {:ng-controller "FacetCtrl"
    :ng-show "state.facets"}
   [:div.panel-heading "Facets"]
   [:div.panel-body
    [:div.row
     {:ng-repeat "(name, facetSet) in state.facets"}
     [:h4 
      {:ng-click "closed = !closed"}
      [:i.fa {:ng-class "{'fa-caret-right': closed, 'fa-caret-down': !closed}"}]
      [:ng-pluralize
       {:count "countFacets(facetSet)"
        :when "{'one': 'One {{name}}', 'other': '{} {{name}}s'}"}]]
     [:div.slide-up {:ng-class "{closed: closed}"}
      [:button.col-xs-12.facet.btn.btn-default.clearfix
       {:blur-on "click"
        :ng-class "{active: info.selected}"
        :ng-repeat "info in facetSet | mappingToArray | orderBy:'count':true"
        :ng-click "info.selected = !info.selected"}
       [:span.pull-right.badge "{{info.count}}"]
       [:span.pull-left        "{{info.$key}}"]]
      ]]]])
