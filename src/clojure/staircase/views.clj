(ns staircase.views
  (:require staircase.views.about
            staircase.views.welcome
            staircase.views.options
            staircase.views.history
            staircase.views.header
            staircase.views.footer
            staircase.views.start
            staircase.views.projects
            [staircase.views.layout :as layout]))

(def vendor-scripts ["jquery/dist/jquery.min.js"])

;; Render a named partial page section. These generally correspond to angularjs
;; page templates.
(defn render-partial
  [config fragment]
  (case fragment
    "about"                (staircase.views.about/snippet config)
    "projects"             (staircase.views.projects/snippet config)
    "frontpage"            (staircase.views.start/starting-points config)
    "starting-point"       (staircase.views.start/starting-point config)
    "edit-step-data"       (staircase.views.options/edit-step-dialogue config)
    "choose-tool-dialogue" (staircase.views.options/choose-tool-dialogue config)
    "options-dialogue"     (staircase.views.options/user-options-dialogue config)
    "history"              (staircase.views.history/snippet config)
    {:status 404})) ;; Plain 404, since this handler called as a service.

(defn four-oh-four [config]
  (layout/common (assoc config :project-title "Page Not Found")
                 [:div#four-oh-four "The page you requested could not be found"]))

(defn index [config]
  (layout/common config
                 [:div {:ng-view ""}]
                 "/js/init"
                 (map (partial str "/vendor/") vendor-scripts)))