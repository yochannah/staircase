(ns staircase.resources.steps
  (:use staircase.protocols
        staircase.helpers
        [clojure.tools.logging :only (info)])
  (:import java.sql.SQLException)
  (:require staircase.sql
            [cheshire.core :as json]
            [staircase.resources.schema :as schema]
            [com.stuartsierra.component :as component]
            [clojure.java.jdbc :as sql]))

(def table-specs (merge schema/history-step schema/steps))

(defn- get-first [m ks]
  (get m (first (filter (set (keys m)) ks))))

(defn- get-prop [m k] (get-first m [k (name k)]))

(defn ensure-string [maybe-string]
  (if (instance? String maybe-string)
    maybe-string
    (json/generate-string maybe-string)))

(defn parse-data [step]
  (update-in step [:data] json/parse-string))

(def parse-steps (comp vec (partial map parse-data)))

(defrecord StepsResource [db]
  component/Lifecycle 

  (start [component]
    (staircase.sql/create-tables
      (:connection db)
      table-specs)
    component)

  (stop [component] component)

  Resource
  
  (get-all [_] (sql/query (:connection db)
                          ["select * from steps"]
                          :result-set-fn parse-steps))

  (exists? [_ id] (staircase.sql/exists (:connection db) :steps id))

  (get-one [_ id]
    (when-let [uuid (string->uuid id)]
      (sql/query
        (:connection db)
        ["select * from steps where id = ?" uuid]
        :result-set-fn (comp parse-data first))))

  (update [_ id doc] (staircase.sql/update-entity (:connection db) :steps id doc))

  (delete [_ id]
    (when-let [uuid (string->uuid id)]
      (sql/with-db-transaction [conn (:connection db)]
        (sql/delete! conn :steps ["id=?" uuid])
        (sql/delete! conn :history_step ["step_id=?" uuid])))
    nil)

  (create [_ doc]
    (let [id (new-id)
          step (-> doc
                   (dissoc "history_id" :history_id :id) 
                   (assoc "id" id)
                   (update-in ["data"] ensure-string))
          link {"history_id" (java.util.UUID/fromString (str (get-prop doc :history_id))) ;; Make sure the id is a uuid
                "step_id" id
                "created_at" (java.sql.Timestamp. (.getTime (now)))}]
      (sql/with-db-transaction [trs (:connection db)]
        (sql/insert! trs :steps step)
        (sql/insert! trs :history_step link))
      id)))

(defn new-steps-resource [& {:keys [db]}] (map->StepsResource {:db db}))
