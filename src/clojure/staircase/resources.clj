(ns staircase.resources
  (:require
    [honeysql.helpers :refer (select from where)]
    [honeysql.core :as hsql]
    [com.stuartsierra.component :as component]))

;; Resources can use this dynamic variable for accessing context, such
;; as the identity of the current resource owner.
(def ^:dynamic context {:user nil})

(defn all-belonging-to [table & [base]]
  (-> (or base (select :*))
      (from table)
      (where [:= :owner :?user])
      (hsql/format :params context)))

(def owned-resource {:where [:and
                             [:= :id :?uuid]
                             [:= :owner :?user]]})

;; Takes a mapping of factories, and when fully 
;; initialised creates all the resources.
(defrecord ResourceManager [factories db]

  component/Lifecycle

  (start [component]
    (reduce (partial apply assoc)
            component
            (for [[k fac] factories]
              [k (fac db)])))

  (stop [component]
    (apply dissoc component (keys factories))))

(defn new-resource-manager
  "Create a new resource manager"
  [factories]
  (map->ResourceManager {:factories factories}))
