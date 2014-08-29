(ns staircase.views.forms
  (:require [hiccup.def :refer (defelem)]))

(defelem label-input-pair [model]
  [[:span {:ng-show "!editing"}
    [:em {:ng-hide model} (clojure.string/replace model #"\w+\." "No ")]
    (str "{{" model "}}")]
   [:input.form-control {:ng-show "editing" :ng-model model}]])

(def search-input
  [:div.input-group
   {:ng-controller "QuickSearchController"}
   ;; TODO: Definitely make placeholders injectable.
   [:input.form-control {:ng-model "searchTerm" :placeholder "enter a search term"}]
   [:span.input-group-btn
    [:button.btn.btn-primary
     {:type "submit" :ng-click "startQuickSearchHistory(searchTerm)"}
     [:span.wordy-label "Search "]
     [:i.fa.fa-search]]]])

(defn search-form []
  [:form.navbar-form.navbar-left {:role "search"} search-input])
