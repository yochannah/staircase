(ns staircase.views
  (:require staircase.views.about
            staircase.views.welcome
            staircase.views.options
            staircase.views.history
            staircase.views.header
            staircase.views.footer
            staircase.views.start
            [hiccup.form :refer :all]
            [clojure.string :as string]
            [hiccup.def :refer (defelem)]
            [persona-kit.view :as pv])
  (:use [hiccup.core :only (html)]
        [hiccup.page :only (html5 include-css include-js)]
        [hiccup.element :only (link-to unordered-list javascript-tag mail-to)]))

(defn- with-utf8-charset [[tag attrs]]
  [tag (assoc attrs :charset "utf-8")])

(def ^:dynamic require-js "/vendor/requirejs/require.js")

(defn- entry-point [ep]
  (->> (include-js require-js)
       (map (fn [[tag attrs]] [tag (assoc attrs :data-main ep)]))
       (first)))

(def vendor-scripts ["jquery/dist/jquery.min.js"])

(defn common [config body & scripts]
  (let [title (:project-title config)]
   (html5
     [:head
      [:meta {:charset "utf-8"}]
      [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge,chrome=1"}]
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1, maximum-scale=1"}]
      [:title title]
      (pv/include-persona)
      (include-css "/css/style.css")
      ]
     (concat [
              [:body {:class "staircase"}
               (staircase.views.header/snippet config)
               [:section#content.main body]]
               (staircase.views.footer/snippet config)
              ]
             scripts))))

(defn four-oh-four [config]
  (common (assoc config :project-title "Page Not Found")
          [:div#four-oh-four "The page you requested could not be found"]))

(def edit-step
  (staircase.views.modals/template "Edit step data"
                  [:form.form
                   [:editable-data {:data "data"}]]))

(def choose-tool-dialogue
  (staircase.views.modals/template "Choose tool"
                  [:ul.list-group
                   [:li.list-group-item
                    {:ng-repeat "item in items"
                     :ng-class "{active: item == selected.item}"}
                    [:a {:ng-click "selected.item = item"} "{{item.heading}}"]]]))

(defn render-partial
  [config fragment]
  (case fragment
    "frontpage" (html (staircase.views.start/starting-points config))
    "starting-point" (html (staircase.views.start/starting-point config))
    "edit-step-data" edit-step
    "choose-tool-dialogue" choose-tool-dialogue
    "options-dialogue" (staircase.views.options/dialogue)
    "history" (html (staircase.views.history/snippet config))
    "about" (html (staircase.views.about/snippet config))
    {:status 404})) 

(defn index [config]
  (common config
          [:div {:ng-view ""}]
          (map with-utf8-charset
               (conj (apply include-js (map (partial str "/vendor/") vendor-scripts))
                     (entry-point "/js/frontpage")))))
