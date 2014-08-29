(ns staircase.views.buttons
  (:require [hiccup.def :refer (defelem)]
            [persona-kit.view :as pv]))

(defelem edit-button []
  [:i.fa.fa-edit.pull-right {:ng-show "!editing" :ng-click "editing = true"}])

(defelem collapse-button []
  [:i.fa.pull-left.collapser
   {:ng-click "collapsed = !collapsed"
    :ng-class "{'fa-caret-right': collapsed, 'fa-caret-down': !collapsed}"}])

(defelem cancel-button []
  [:i.fa.fa-undo.pull-right {:ng-show "editing"}])

(defelem save-button []
  [:i.fa.fa-save.pull-right {:ng-show "editing"}])

(def ^:dynamic *button-style* :blue) ;; :orange or :dark

(defn login []
  (-> (pv/sign-in-button {:ng-show "persona" :ng-click "persona.request()"} *button-style*)
      (update-in [1 :class] str " navbar-btn")
      (update-in [2 1] (constantly "Sign in/Sign up"))))

(defn logout []
  (-> (pv/sign-in-button {:ng-show "persona" :ng-click "persona.logout()"} *button-style*)
      (update-in [1 :class] str " navbar-btn")
      (update-in [1 :title] (constantly "Signed in as {{auth.identity}}"))
      (update-in [2 1] (constantly "Sign out"))))
