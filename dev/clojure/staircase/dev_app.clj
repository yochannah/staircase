(ns staircase.dev-app
  (:use [clojure.tools.logging :only (debug info error)]
        [org.httpkit.server :only (run-server)])
  (:require [environ.core :refer (env)]
            [com.stuartsierra.component :as component]
            [staircase.assets :as assets]
            [staircase.app :as app]))

(declare system)

(defn asset-pipeline [options]
  (assets/pipeline :js-dir "/js"
                  :css-dir "/css"
                  :engine (:asset-js-engine options)
                  :max-age (:web-max-age options)
                  :as-resource "tools"
                  :coffee "src/coffee"
                  :ls     "src/ls"
                  :less   "src/less"))

(def ^{:doc "Dev application handler"} dev-handler
  (delay
    (get-in (system) [:router :handler])))

(defn handler [req]
  (@dev-handler req))

(defn system
  "Create a development application"
  []
  (info "Building dev system")
  (-> (app/build-app env)
      (update-in [:router :middlewares]
                 conj (asset-pipeline env))
      (component/start)))

(defn start []
  (let [port (Integer/parseInt (get env :port "3000"))]
    (run-server @dev-handler {:port port})
    (info "Started dev server on port" port)))
