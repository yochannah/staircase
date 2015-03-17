(ns staircase.app
  (:use [clojure.tools.logging :only (debug info error)]
        [environ.core :only (env)]
        [staircase.config :only (db-options client-options app-options secrets)]
        staircase.resources.services
        staircase.resources.histories
        staircase.resources.steps
        [staircase.resources.mymine :only (new-mymine-resource)])
  (:require [staircase.assets :as assets]
            [staircase.handler :as routing]
            [com.stuartsierra.component :as component]
            [staircase.sessions :as sessions]
            [staircase.data :as data]))

(declare system)

;; Inject dependencies and build up the system.
(defn build-app [options]
  (. (Runtime/getRuntime)
     (addShutdownHook (Thread. (fn [] (component/stop @system)))))
  (component/start
    (let [db (db-options options)]
      (-> (component/system-map
            :asset-pipeline (assets/pipeline :js-dir "/js"
                                             :css-dir "/css"
                                             :engine :v8
                                             :max-age (:web-max-age options)
                                             :as-resource "tools"
                                             :coffee "src/coffee"
                                             :ls     "src/ls"
                                             :less   "src/less")
            :config (atom (assoc (app-options options) ;; Allow config to change at run-time by atomising it.
                                  :client (client-options options)))
            :secrets (secrets options)
            :session-store (sessions/new-pg-session-store)
            :db (data/new-pooled-db db)
            :histories (new-history-resource)
            :services (new-services-resource)
            :steps (new-steps-resource)
            :projects (new-mymine-resource)
            :router (routing/new-router))
          (component/system-using
            {:router [:config :secrets :session-store :services :histories :steps :asset-pipeline]
             :session-store [:db :config]
             :services [:db]
             :steps [:db]
             :projects [:db]
             :histories [:db]})))))

;; Todo - should be possible to have multiple instances.
(def system
  (delay
    (info "Building system with settings: " env)
    (build-app env)))

(defn handler [req]
  ((get-in @system [:router :handler]) req))

