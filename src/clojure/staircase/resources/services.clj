(ns staircase.resources.services
  (:use staircase.protocols
        staircase.helpers
        [clojure.tools.logging :only (info)])
  (:require staircase.sql
            staircase.resources
            [staircase.resources.schema :as schema]
            [com.stuartsierra.component :as component]
            [honeysql.helpers :refer (select from where)]
            [honeysql.core :as hsql]
            [clojure.java.jdbc :as sql]))

(def get-service
  (-> staircase.resources/owned-resource
      (from :services)))

(defrecord ServicesResource [db]
  component/Lifecycle 

  (start [component]
    (staircase.sql/create-tables
      (:connection db)
      schema/services)
    component)

  (stop [component] component)

  Resource
  
  (get-all [_] (into [] (sql/query (:connection db)
                                   (-> (select :*)
                                       (from :services)
                                       (where [:= :owner :?user])
                                       (hsql/format :params staircase.resources/context)))))

  (exists? [_ id] (staircase.sql/exists-with-owner :services id (:user staircase.resources/context)))

  (get-one
    [_ id]
    (when-let [uuid (string->uuid id)]
      (let [params (merge staircase.resources/context {:uuid uuid})]
        (sql/query (:connection db)
                   (-> get-service
                       (select :*)
                       (hsql/format :params params))
                   :result-set-fn first))))

  (update [_ id doc] (staircase.sql/update-owned-entity
                       (:connection db)
                       :services
                       id (:user staircase.resources/context)
                       doc))

  (delete [_ id]
    (when-let [uuid (string->uuid id)]
      (sql/delete! (:connection db)
                   :services
                   ["id=? and owner=?" uuid (:user staircase.resources/context)]))
    nil)

  (create [_ doc]
    (let [id (new-id)
          owner (:user staircase.resources/context)
          service (-> (dissoc doc :id "id") (assoc "id" id "owner" owner))]
      (sql/with-db-transaction [trs (:connection db)]
        (sql/delete! trs :services ["root=? and owner=?" (:root service) owner]) ;; Preserve invariant.
        (sql/insert! trs :services service))
      id))
  
  Searchable

  (get-where [_ constraint]
    (sql/query (:connection db)
               (hsql/format {:select [:*]
                             :from   [:services]
                             :where  [:and constraint [:= :owner :?user]]}
                            :params staircase.resources/context)
               :result-set-fn vec))
  )

(defn new-services-resource [& {:keys [db]}] (map->ServicesResource {:db db}))
