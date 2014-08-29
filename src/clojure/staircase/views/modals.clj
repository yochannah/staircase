(ns staircase.views.modals
    (:use [hiccup.core :only (html)])
    (:require [clojure.string :as string]))

(defn template ;; template for a dialogue whose controller provides close and ok methods.
  ([title body-content]
   (template title
                   (->> (string/split title #" ")
                        (map string/lower-case)
                        (string/join "-"))
                   body-content))
  ([title body-class body-content]
   (html
     [:div.modal-header
      [:h3.modal-title
       [:button.btn.btn-warning.pull-right {:ng-click "cancel()"} "close"]
       title]]
     [:div.modal-body {:class body-class} body-content]
     [:div.modal-footer
      [:button.btn.btn-warning {:ng-click "cancel()"} "cancel"]
      [:button.btn.btn-primary {:ng-click "ok()"} "OK"]
      ])))
