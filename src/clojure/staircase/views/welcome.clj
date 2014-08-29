(ns staircase.views.welcome
  (:require staircase.views.about))

(defn snippet [config]
  [:div.row.welcome {:ng-controller "WelcomeCtrl" :ng-show "showWelcome"}
   [:div
    [:div
     (staircase.views.about/about-header config) ]]])
