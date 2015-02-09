(ns staircase.views.layout
  (:use [hiccup.page :only (html5 include-js include-css)])
  (:require staircase.views.header
            staircase.views.footer
            [persona-kit.view :as pv]))

(defn- with-utf8-charset [script]
  (update-in script [1] assoc :charset "utf-8"))

(def ^:dynamic require-js "/vendor/requirejs/require.js")

(defn entry-point [ep]
  (for [script (include-js require-js)]
    (update-in script [1] assoc :data-main ep)))

(defn common
  ([config body] (common config body nil []))
  ([config body main scripts]
   (let [title (:project-title config)
         js (concat (apply include-js scripts) (entry-point main))]
     (html5
       [:head
        [:meta {:charset "utf-8"}]
        [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge,chrome=1"}]
        [:meta {:name "viewport" :content "width=device-width, initial-scale=1, maximum-scale=1"}]
        [:title title]
        (pv/include-persona)
        (include-css "/css/style.css")
        (include-css "/vendor/angular-xeditable/dist/css/xeditable.css")
        ]
       [:body {:class "staircase"}
        (staircase.views.header/snippet config)
        [:section#content.main body]
        ; (staircase.views.footer/snippet config)
        (map with-utf8-charset js)]))))

