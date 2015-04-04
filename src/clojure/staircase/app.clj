;; The purpose of this namespace is to construct the system
;; from its various parts, wiring them together into a whole.
;; It is thus the ideal place to declare dependencies and
;; construct instances of things we need. For details of this
;; style of doing things, see component and env.
(ns staircase.app
  (:use [clojure.tools.logging :only (debug info error)]
        [environ.core :only (env)]
        [staircase.config :only (db-options client-options app-options secrets)]
        [staircase.resources :only (new-resource-manager)]
        [staircase.resources.services :only (new-services-resource)]
        [staircase.resources.histories :only (new-history-resource)]
        [staircase.resources.steps :only (new-steps-resource)]
        [staircase.resources.projects :only (new-projects-resource)])
  (:require [staircase.handler :as routing]
            [com.stuartsierra.component :as component]
            [staircase.sessions :as sessions]
            [staircase.data :as data]))

(declare system)

(defn handler
  "Get a ring handler"
  []
  (get-in (system) [:router :handler]))

(def resource-manager
  (new-resource-manager
    {:histories new-history-resource
     :services  new-services-resource
     :steps     new-steps-resource
     :projects  new-projects-resource}))

(def dependency-graph
  {:router        [:config :secrets :session-store :resources]
   :session-store [:db :config]
   :resources     [:db]})

(defn get-config [options]
  (atom (assoc (app-options options) ;; Allow config to change at run-time by atomising it.
               :client (client-options options))))

;; Inject dependencies and build up the system.
;; Usually options are read in from the environment
;; using a configuration system, but it is just a
;; basic map from keyword => config value..
(defn build-app [options]
  ;; Ensure our system is shutdown when the VM does.
  (debug "System settings:" options)
  (let [sys (-> (component/system-map
                  :config (get-config options)
                  :db (data/new-pooled-db (db-options options))
                  :resources resource-manager
                  :router (routing/new-router)
                  :secrets (secrets options)
                  :session-store (sessions/new-pg-session-store))
                (component/system-using dependency-graph))]
    (. (Runtime/getRuntime) ;; Ensure we stop this system.
       (addShutdownHook (Thread. (fn [] (component/stop sys)))))
    sys))

(defn system
  "Create an application"
  []
  (info "Building system")
  (component/start (build-app env)))

