(ns staircase.app
  (:use [clojure.tools.logging :only (debug info error)])
  (:require [staircase.handler :as routing]
            [com.stuartsierra.component :as component]
            [staircase.data :as data]
            [staircase.config]))

(def conf (staircase.config/config (or (System/getenv "ENVIRONMENT") :development)))

(info "Config:" conf)

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

(def handler (get-in (build-app conf) [:router :handler]))

