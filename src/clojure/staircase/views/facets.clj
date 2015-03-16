(ns staircase.views.facets
  (:require [hiccup.def :refer (defelem)]))

(defelem snippet []
  [:div.facets.panel.panel-default
   {:ng-controller "FacetCtrl"
    :ng-show "state.facets"}
   [:div.panel-heading "Facets"]
   [:div.panel-body
    [:div
     {:ng-repeat "(name, facetSet) in state.facets"}
     [:h4 
      {:ng-click "closed = !closed"}
      [:i.fa {:ng-class "{'fa-caret-right': closed, 'fa-caret-down': !closed}"}]
      "{{ name | pluralizeWithNum:countFacets(facetSet) }}"]
     [:div.list-group
      [:a.list-group-item
       {:blur-on "click"
        :ng-class "{closed: closed, active: info.selected}"
        :ng-repeat "info in facetSet | mappingToArray | orderBy:'count':true"
        :ng-click "info.selected = !info.selected"}
       [:span.badge "{{info.count}}"]
       [:span       "{{info.$key}}"]]
      ]]]])
