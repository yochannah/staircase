(ns staircase.app
  (:use [clojure.tools.logging :only (debug info error)]
        [environ.core :only (env)]
        [staircase.config :only (db-options)]
        staircase.resources.histories
        staircase.resources.steps)
  (:require [staircase.handler :as routing]
            [com.stuartsierra.component :as component]
            [staircase.data :as data]))

;; Inject dependencies and build up the system.
(defn build-app [options]
  (component/start
    (let [db (db-options options)]
      (-> (component/system-map
            :db (data/new-pooled-db db)
            :histories (new-history-resource)
            :steps (new-steps-resource)
            :router (routing/new-router))
          (component/system-using
            {:router [:histories :steps]
            :steps [:db]
            :histories [:db]})))))

(def system
  (delay
    (info "Building system with settings: " env)
    (build-app env)))

(defn handler [req]
  ((get-in @system [:router :handler]) req))

