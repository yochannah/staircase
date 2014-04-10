(ns staircase.resources.histories
  (:use staircase.protocols
        staircase.helpers
        [clojure.tools.logging :only (info)])
  (:import java.sql.SQLException)
  (:require staircase.sql
            [com.stuartsierra.component :as component]
            [clojure.java.jdbc :as sql]))

(defn- build-history [row & rows]
  (let [init (-> row
                 (assoc :steps (if (:step_id row) [(:step_id row)] []))
                 (dissoc :step_id))
        f #(update-in %1 [:steps] conj (:step_id %2))]
    (reduce f init rows)))

(defrecord HistoryResource [db]
  component/Lifecycle

  (start [component]
    (staircase.sql/create-tables
      (:connection db)
      {
       :histories [ [:id :uuid "primary key"] [:title "varchar(1024)"] [:text :varchar] ]
       :history_step [ [:history_id :uuid] [:created_at :timestamp] [:step_id :uuid] ]
       })
    component)

  (stop [component] component)

  Resource

  (get-all [histories]
    (into [] (sql/query (:connection db) ["select * from histories"])))

  (exists? [_ id] (staircase.sql/exists (:connection db) :histories id))

  (get-one [histories id]
    (when-let [uuid (string->uuid id)]
      (sql/query
        (:connection db)
        [" select h.*, hs.step_id
           from histories as h
           left join history_step as hs on h.id = hs.history_id
           where h.id = ?
           order by hs.created_at desc " uuid] 
        :result-set-fn #(if (empty? %) nil (apply build-history %)))))

  (update [_ id doc] (staircase.sql/update-entity (:connection db) :histories id doc))

  (delete [histories id]
    (when-let [uuid (string->uuid id)]
      (sql/with-db-transaction [conn (:connection db)]
        (sql/delete! conn :histories ["id=?" uuid])
        (sql/delete! conn :history_step ["history_id=?" uuid])))
    nil)

  (create [histories doc]
    (let [id (new-id)
          values (assoc doc "id" id)]
      (sql/insert! (:connection db) :histories values)
      id)))

(defn new-history-resource [& {:keys [db]}] (map->HistoryResource {:db db}))

