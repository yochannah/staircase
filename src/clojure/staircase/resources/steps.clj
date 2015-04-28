(ns staircase.resources.steps
  (:use staircase.protocols
        staircase.helpers
        [clojure.tools.logging :only (info)])
  (:import java.sql.SQLException)
  (:require staircase.sql
            clojure.string
            [cheshire.core :as json]
            [clojure.java.jdbc :as sql]))

;; Get the value in the mapping of the first key for
;; which a value exists in the mapping.
(defn- get-first [m ks]
  (get m (-> (keys m)
             set
             (filter ks)
             first)))

;; Get the value of k, either as a keyword or string.
(defn- get-prop [m k] (get-first m [k (name k)]))

;; Make sure the thing we have is a string.
(defn ensure-string [maybe-string]
  (if (instance? String maybe-string)
    maybe-string
    (json/generate-string maybe-string)))

;; Transformations can be put in here on the stored step data.
;; At the moment we just make sure we parse the data from JSON.
(defn parse-data [step] 
  (when step
    (update-in step [:data] json/parse-string)))

;; Parse a collection of step rows into a vector of steps.
(def parse-steps (comp vec (partial map parse-data)))

(defrecord StepsResource [db]

  Resource
  
  (get-all [_] (sql/query db
                          ["select * from steps"]
                          :result-set-fn parse-steps))

  (exists? [_ id] (staircase.sql/exists db :steps id))

  (get-one [_ id]
    (when-let [uuid (string->uuid id)]
      (sql/query db
        ["select * from steps where id = ?" uuid]
        :result-set-fn (comp parse-data first))))

  (update [_ id doc]
    (staircase.sql/update-entity db :steps id doc))

  (delete [_ id]
    (when-let [uuid (string->uuid id)]
      (sql/with-db-transaction [trs db]
        (sql/delete! trs :steps ["id=?" uuid])
        (sql/delete! trs :history_step ["step_id=?" uuid])))
    nil)

  (create [_ doc] ;; TODO: reuse existing steps where possible...
    "Creates a step and links it to its history."
    (let [step (-> doc
                   stringly-keyed
                   (dissoc "history_id" "id") 
                   (update-in ["data"] ensure-string))
          history-id (java.util.UUID/fromString
                       (str (get-prop doc :history_id)))]
      (sql/with-db-transaction [trs db]
        (let [step-id (-> (sql/insert! trs :steps step) first :id)]
          (sql/insert! trs ;; Now link it to the history.
                       :history_step ;; Foreign keys make sure that the history exists.
                       {"history_id" history-id "step_id" step-id})
          step-id)))))

(defn new-steps-resource [db] (map->StepsResource {:db db}))
