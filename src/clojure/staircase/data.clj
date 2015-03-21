(ns staircase.data
  (:import com.mchange.v2.c3p0.ComboPooledDataSource)
  (:use staircase.helpers
        [clojure.tools.logging :only (debug info error)])
  (:require staircase.resources
            [staircase.migrations :as migrations]
            [staircase.resources.steps :as steps]
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
                (.setJdbcUrl (str "jdbc:" (:subprotocol config) ":" (:subname config)))))
        {:datasource cpds}))

(defrecord PooledDatabase [config connection]
  component/Lifecycle

  (start [component]
    (info "Starting pooled database" (prn-str config))
    (let [pool (open-pool config)]
      (when (:migrate config)
        (info "Migrating DB")
        (migrations/migrate (:datasource pool)))
      (assoc component :connection (open-pool config))))

  (stop [component]
    (info "Stopping pooled database")
    (.close (:datasource connection))
    (dissoc component :connection)))

(defn new-pooled-db [config]
  (info "Creating new pooled db with config: " config)
  (map->PooledDatabase {:config config}))

(defn add-all-steps [{db :db} hid steps]
  (apply sql/insert! (:connection db) :history_step (map #(assoc %1 :history_id hid) steps)))

(defn get-history-steps-of [{db :db} hid limit]
  (let [query (str "select * from history_step where history_id = ? order by created_at ASC LIMIT " limit)]
    (sql/query (:connection db) [query (string->uuid hid)] :result-set-fn vec)))

(defn get-steps-of [{db :db} id & {:keys [limit] :or {limit nil}}]
  (when-let [uuid (string->uuid id)]
    (let [query-base "select s.*
                     from steps as s
                     left join history_step as hs on s.id = hs.step_id
                     left join histories as h on h.id = hs.history_id
                     where hs.history_id = ? and h.owner = ?
                     order by hs.created_at ASC"
          limit-clause (if limit (str " LIMIT " limit) "")
          query (str query-base limit-clause)]
        (sql/query (:connection db)
                   [query uuid (:user staircase.resources/context)]
                   :result-set-fn steps/parse-steps))))
