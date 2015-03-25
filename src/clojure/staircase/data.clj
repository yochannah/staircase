(ns staircase.data
  (:import com.mchange.v2.c3p0.ComboPooledDataSource)
  (:use staircase.helpers
        [clojure.tools.logging :only (debug info error)])
  (:require [staircase.migrations :as migrations]
            [com.stuartsierra.component :as component]
            [clojure.java.jdbc :as sql]))

(defn- open-pool
  [config]
  (let [cpds (doto (ComboPooledDataSource.)
               (.setDriverClass (:classname config))
               (.setMaxPoolSize 6)
               (.setMinPoolSize 1)
               (.setInitialPoolSize 1))]
    (if-let [uri (:connection-uri config)]
      (.setJdbcUrl cpds uri)
      (doto cpds
        (.setUser (:user config))
        (.setPassword (:password config))
        (.setJdbcUrl
          (str "jdbc:"
               (:subprotocol config) ":" (:subname config)))))
    cpds))

(defrecord PooledDatabase [config datasource]
  component/Lifecycle

  (start [component]
    (info "Starting pooled database" (prn-str config))
    (let [pool (open-pool config)]
      (when (:migrate config)
        (info "Migrating DB")
        (migrations/migrate pool))
      (assoc component :datasource pool)))

  ;; We want to return a component from stop, so we
  ;; have to wrap it in a call to the constructor since
  ;; assoc returns a record but dissoc does not.
  (stop [component]
    (info "Stopping pooled database")
    (.close datasource)
    (map->PooledDatabase (dissoc component :datasource))))

(defn new-pooled-db [config]
  (info "Creating new pooled db with config: " config)
  (map->PooledDatabase {:config config}))

