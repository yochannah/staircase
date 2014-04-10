(ns staircase.resources.steps
  (:use staircase.protocols
        staircase.helpers
        [clojure.tools.logging :only (info)])
  (:import java.sql.SQLException)
  (:require staircase.sql
            [com.stuartsierra.component :as component]
            [clojure.java.jdbc :as sql]))

(defrecord StepsResource [db]
  component/Lifecycle 

  (start [component]
    (staircase.sql/create-tables
      (:connection db)
      {
       :history_step [ [:history_id :uuid] [:created_at :timestamp] [:step_id :uuid] ]
       :steps [ [:id :uuid :primary :key] [:title "varchar(1024)"] [:tool "varchar(1024)"] [:data :text] ]
       })
    component)

  (stop [component] component)

  Resource
  
  (get-all [_] (into [] (sql/query (:connection db) ["select * from steps"])))

  (exists? [_ id] (staircase.sql/exists (:connection db) :steps id))

  (get-one [_ id]
    (when-let [uuid (string->uuid id)]
      (sql/query
        (:connection db)
        ["select * from steps where id = ?" uuid]
        :result-set-fn first)))

  (update [_ id doc] (staircase.sql/update-entity (:connection db) :steps id doc))

  (delete [_ id]
    (when-let [uuid (string->uuid id)]
      (sql/with-db-transaction [conn (:connection db)]
        (sql/delete! conn :steps ["id=?" uuid])
        (sql/delete! conn :history_step ["step_id=?" uuid])))
    nil)

  (create [_ doc]
    (let [id (new-id)
          step (-> doc (dissoc "history_id") (assoc "id" id))
          link {"history_id" (java.util.UUID/fromString (str (doc "history_id"))) ;; Make sure the id is a uuid
                "step_id" id
                "created_at" (java.sql.Date. (.getTime (now)))}]
      (sql/with-db-transaction [trs (:connection db)]
        (sql/insert! trs :steps step)
        (sql/insert! trs :history_step link))
      id)))

(defn new-steps-resource [& {:keys [db]}] (map->StepsResource {:db db}))
