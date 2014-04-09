(ns staircase.app
  (:use [clojure.tools.logging :only (debug info error)])
  (:require [staircase.handler :as routing]
            [com.stuartsierra.component :as component]
            [staircase.data :as data]
            [staircase.config]))

(defn get-conf [] (staircase.config/config (or (System/getenv "ENVIRONMENT") :development)))

;; Inject dependencies and build up the system.
(defn build-app [options]
  (component/start
    (let [{:keys [db]} options]
      (-> (component/system-map
            :db (data/new-pooled-db db)
            :histories (data/new-history-resource)
            :steps (data/new-steps-resource)
            :router (routing/new-router))
          (component/system-using
            {:router [:histories :steps]
            :steps [:db]
            :histories [:db]})))))

(def system (delay
              (info "Building system")
              (let [conf (get-conf)]
                (info "config: " conf)
                (build-app conf))))

(defn handler [req]
  ((get-in @system [:router :handler]) req))

