(ns staircase.data
  (:import com.mchange.v2.c3p0.ComboPooledDataSource)
  (:import java.sql.SQLException)
  (:use staircase.protocols [clojure.tools.logging :only (debug info error)])
  (:require [com.stuartsierra.component :as component]
            [clojure.java.jdbc :as sql]))

(defn open-pool
      [config]
      (let [cpds (doto (ComboPooledDataSource.)
                   (.setDriverClass (:classname config))
                   (.setJdbcUrl (str "jdbc:" (:subprotocol config) ":" (:subname config)))
                   (.setUser (:user config))
                   (.setPassword (:password config))
                   (.setMaxPoolSize 6)
                   (.setMinPoolSize 1)
                   (.setInitialPoolSize 1))]
        {:datasource cpds}))

(defrecord PooledDatabase [config connection]
  component/Lifecycle

  (start [component]
    (info "Starting pooled database")
    (assoc component :connection (open-pool config)))

  (stop [component]
    (info "Stopping pooled database")
    (.close (:datasource connection))
    (dissoc component :connection)))

(defn new-pooled-db [config]
  (info "Creating new pooled db with config: " config)
  (map->PooledDatabase {:config config}))

(defn new-id [] (str (java.util.UUID/randomUUID)))

(defn string->uuid [name] 
  (try
    (java.util.UUID/fromString name)
    (catch IllegalArgumentException e nil)))

(defn now [] (java.util.Date.))

(defn build-history [row & rows]
  (let [init (-> row
                 (assoc :steps [(:step_id row)])
                 (dissoc :step_id))
        f #(update-in %1 [:steps] conj (:step_id %2))]
    (reduce f init rows)))

(defmacro get-sql-metadata [db method & args]
  `(sql/with-connection ~db
     (doall
       (sql/resultset-seq
         (~method
           (.getMetaData (sql/connection))
           ~@args)))))

;; Get the set of tables names.
(defn get-table-names [db]
  (into #{} (map :table_name (get-sql-metadata db .getTables nil nil nil (into-array ["TABLE" "VIEW"])))))

(defrecord HistoryResource [db]
  component/Lifecycle

  (start [component]
    (let [tables (get-table-names (:connection db))]
      (when (not (contains? tables "histories"))
        (sql/with-connection (:connection db)
          (info "Creating table: histories")
          (sql/create-table :histories
                            [:id "uuid" "primary key"]
                            [:title "varchar(1024)"]
                            [:text :varchar]))))
    component)

  (stop [component] component)

  Resource

  (get-all [histories]
    (sql/with-connection (:connection db)
      (sql/with-query-results results
        ["select * from histories"]
        (into [] results))))

  (exists? [_ id]
    (if-let [uuid (string->uuid id)]
      (sql/with-connection (:connection db)
        (sql/with-query-results results
          ["select count(*) as n from histories where id = ?", uuid]
          (get-in results [0 :n])))
      false))

  (get-one [histories id]
    (when-let [uuid (string->uuid id)]
      (sql/with-connection (:connection db)
        (sql/with-query-results results
          ["select h.*, hs.step_id
            from histories as h
            left join history_step as hs on h.id = hs.history_id
            where h.id = ?
            order by hs.created_at desc
          " uuid]
          (when results (apply build-history results))))))

  (update [histories id doc]
    (let [uuid (java.util.UUID/fromString id)
          document (assoc doc "id" uuid)]
      (sql/with-connection (:connection db)
        (sql/update-values :histories ["id=?" uuid] document))
      document))

  (delete [histories id]
    (let [uuid (java.util.UUID/fromString id)]
      (sql/with-connection (:connection db)
        (sql/delete-rows :histories ["id=?" uuid]))))

  (create [histories doc]
    (let [id (new-id)
          values (assoc doc "id" id)]
      (sql/with-connection (:connection db)
        (sql/insert-record :histories values))
      id)))

(defn new-history-resource [] (map->HistoryResource {}))

(defn log-sql-error [e]
  (error e)
  (when-let [ne (.getNextException e)]
    (log-sql-error ne)))

(defrecord StepsResource [db]
  component/Lifecycle 

  (start [component]
    (let [conn (:connection db)
          tables (get-table-names conn)]
      (sql/with-connection conn
        (when (not (contains? tables "history_step"))
          (info "Creating table history_step")
          (try
            (sql/create-table :history_step
                              [:history_id "uuid"]
                              [:created_at "timestamp"]
                              [:step_id "uuid"])
            (catch SQLException e (do (log-sql-error e) (throw e)))))
        (when (not (contains? tables "steps"))
          (info "Creating table steps")
          (sql/create-table :steps
                            [:id "uuid" "primary key"]
                            [:title "varchar(1024)"]
                            [:tool "varchar(1024)"]
                            [:data :varchar]))))
    component)

  (stop [component] component)

  Resource
  
  (get-all [steps]
    (sql/with-connection (:connection db)
      (sql/with-query-results results
        ["select * from steps"]
        (into [] results))))

  (exists? [_ id]
    (sql/with-connection (:connection db)
      (sql/with-query-results results
        ["select count(*) as n from steps where id = ?", id]
        (get-in results [0 :n]))))

  (get-one [steps id]
    (sql/with-connection (:connection db)
      (sql/with-query-results results
        ["select *
           from steps
           where id = ?" id]
        (when results (first results)))))

  (update [steps id doc]
    (let [document (assoc doc "id" id)]
      (sql/with-connection (:connection db)
        (sql/update-values :steps ["id=?" id] document))
      document))

  (delete [_ id]
    (sql/with-connection (:connection db)
      (sql/delete-rows :steps ["id=?" id])))

  (create [_ doc]
    (let [id (new-id)
          step (dissoc "history_id" (assoc doc "id" id))
          link {"history_id" (java.util.UUID/fromString (doc "history_id"))
                "step_id" id
                "created_at" (now)}]
      (sql/with-connection (:connection db)
        (sql/transaction
          (sql/insert-record :steps step)
          (sql/insert-record :history_step link)))
      id)))

(defn new-steps-resource [] (map->StepsResource {}))

(defn get-steps-of [{db :db} id & {:keys [limit] :or {limit nil}}]
  (when-let [uuid (string->uuid id)]
    (let [query-base "select s.* from steps as s, history_step as hs where hs.history_id = ? order by hs.created_at DESC"
          limit-clause (if limit (str " LIMIT " limit) "")
          query (str query-base limit-clause)]
      (sql/with-connection (:connection db)
        (sql/with-query-results results
          [query uuid]
          (into [] results))))))
