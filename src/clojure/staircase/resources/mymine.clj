(ns staircase.resources.mymine
  (:use staircase.protocols
        staircase.helpers
        [clojure.tools.logging :only (info)])
  (:import java.sql.SQLException)
  (:require staircase.sql
            clojure.string
            [cheshire.core :as json]
            [staircase.resources.schema :as schema]
            [com.stuartsierra.component :as component]
            [clojure.java.jdbc :as sql]))

(def table-specs (merge schema/project-contents schema/projects))

(defrecord MyMineResource [db]
  component/Lifecycle 

  (start [component]
    (let [conn (:connection db)]
      (staircase.sql/create-tables conn table-specs))
    component)

  (stop [component] component)

  Resource
  
  (exists? [_ id] (staircase.sql/exists (:connection db) :projects id)))

(defn new-mymine-resource [& {:keys [db]}] (map->MyMineResource {:db db}))