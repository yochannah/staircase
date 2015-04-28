(ns staircase.repl
  "Helpers for working in the REPL"
  (:require [com.stuartsierra.component :as component]
            [staircase.data :as data]
            [environ.core :refer (env)]
            [staircase.config :as conf]))

(defn get-db [db-name]
  """
    Helper for getting access to a db on the repl.
    ----------------------------------------------
    When connecting to a webapp repl you should access the
    db found on the app system in staircase.app/system
  """
  (let [db-conf (assoc (conf/db-options env) :subname (str "//localhost/" db-name))]
    (component/start (data/new-pooled-db db-conf))))

