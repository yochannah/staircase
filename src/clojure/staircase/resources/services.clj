(ns staircase.resources.services
  (:use staircase.protocols
        staircase.helpers
        [clojure.tools.logging :only (info)])
  (:require staircase.sql
            [staircase.resources :as res]
            [com.stuartsierra.component :as component]
            [honeysql.helpers :refer (select from where)]
            [honeysql.core :as hsql]
            [clojure.java.jdbc :as sql]))

(def get-service
  (-> staircase.resources/owned-resource
      (from :services)))

(def ONE_DAY (* 24 60 60 1e3)) ;; One day in milli-seconds

(defn one-day-from-now []
  (java.sql.Timestamp. (+ (System/currentTimeMillis) ONE_DAY)))

(defrecord ServicesResource [db]

  Resource
  
  (get-all [_]
    (sql/query db
               (res/all-belonging-to :services)
               :result-set-fn vector))

  (exists? [_ id]
    (staircase.sql/exists-with-owner db 
                                     :services
                                     (assoc res/context :id id)))

  (get-one
    [_ id]
    (when-let [uuid (string->uuid id)]
      (let [params (assoc res/context :uuid uuid)]
        (sql/query db
                   (-> get-service
                       (select :*)
                       (hsql/format :params params))
                   :result-set-fn first))))

  (update [_ id doc]
    (staircase.sql/update-owned-entity
      db
      :services
      (assoc res/context :id id)
      (if (:token doc) ;; whenever we set the token, set the expiry.
        (assoc doc :valid_until (one-day-from-now))
        doc)))

  (delete [_ id]
    (staircase.sql/delete-entity db :services id))

  (create [_ doc]
    (let [owner  (:user res/context)
          values (-> doc
                     (dissoc "id" :id :owner :valid_until)
                     (assoc "valid_until" (one-day-from-now)
                            "owner" owner))]
      (first (sql/insert! db :services values))))
  
  Searchable

  (get-where [_ constraint]
    (sql/query db
               (hsql/format {:select [:*]
                             :from   [:services]
                             :where  [:and constraint
                                           [:= :owner :?user]]}
                            :params res/context)
               :result-set-fn vec))
  )

(defn new-services-resource [db] (map->ServicesResource {:db db}))
