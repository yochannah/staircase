(ns staircase.resources.histories
  (:use staircase.protocols
        staircase.helpers
        [clojure.tools.logging :only (info)])
  (:require staircase.sql
            staircase.resources
            [honeysql.helpers :refer
              (select merge-select from where merge-where
               left-join group order-by merge-order-by)]
            [honeysql.core :as hsql]
            [clojure.java.jdbc :as sql]))

(defn- build-history [row & rows]
  (let [init (-> row
                 (assoc :steps (if (:step_id row) [(:step_id row)] []))
                 (dissoc :step_id))
        f #(update-in %1 [:steps] conj (:step_id %2))]
    (reduce f init rows)))

(defn- base-history-query []
  (-> (select :h.id :h.title :h.created_at :h.description :h.owner)
      (from [:histories :h])
      (left-join [:history_step :hs] [:= :h.id :hs.history_id])
      (where [:= :h.owner :?user])
      (order-by [:h.created_at :desc])))

(defn- all-history-query []
  (-> (base-history-query)
      (merge-select [:%count.hs.* :steps])
      (group :h.id :h.title :h.created_at :h.description :h.owner)
      (hsql/format :params staircase.resources/context)))

(defn- one-history-query [history]
  (-> (base-history-query)
      (merge-select :hs.step_id)
      (merge-where [:= :h.id :?history])
      (merge-order-by [:hs.created_at :desc])
      (hsql/format :params (assoc staircase.resources/context :history history))))

;; Functions that operate on a history resource, but do
;; their own direct data access.

(defn add-all-steps [{db :db} hid steps]
  (apply sql/insert! db :history_step (map #(assoc %1 :history_id hid) steps)))

(defn get-history-steps-of [{db :db} hid limit]
  (let [query (str "select *
                    from history_step
                    where history_id = ?
                    order by created_at ASC
                    LIMIT " limit)]
    (sql/query db [query (string->uuid hid)] :result-set-fn vec)))

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
        (sql/query db
                   [query uuid (:user staircase.resources/context)]
                   :result-set-fn steps/parse-steps))))

;; The history resource definition.
(defrecord HistoryResource [db]

  Resource

  (get-all [histories]
    (sql/query db (all-history-query)))

  (exists? [_ id]
    (staircase.sql/exists-with-owner
      db
      :histories
      (assoc staircase.resources/context :id id)))

  (get-one [histories id]
    (when-let [uuid (string->uuid id)]
      (sql/query
        db
        (one-history-query uuid)
        :result-set-fn #(if (empty? %) nil (apply build-history %)))))

  (update [_ id doc]
    (staircase.sql/update-owned-entity
      db
      :histories
      (assoc staircase.resources/context :id id)
      (dissoc doc :created_at "created_at")))

  (delete [histories id]
    (when-let [uuid (string->uuid id)]
      (sql/with-db-transaction [trs db]
        (sql/delete! trs :history_step
                     ["history_id=?" uuid])
        (sql/delete! trs :histories
                     ["id=? and owner=?" uuid (:user staircase.resources/context)])
        ;; TODO - delete orphaned steps?
        ))
    nil)

  (create [histories doc]
    (let [owner  (:user staircase.resources/context)
          values (-> doc
                     (dissoc :owner "created_at" :created_at)
                     (assoc "owner" owner))]
      (first (sql/insert! db :histories values))))

(defn new-history-resource [db] (map->HistoryResource {:db db}))

