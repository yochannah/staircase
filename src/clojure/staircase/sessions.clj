;; Session component - thin wrapper over ring-jdbc-session
(ns staircase.sessions
  (:use ring.middleware.session.store
        [clojure.tools.logging :only (info warn debug)])
  (:require [ring-jdbc-session.core :as ring-jdbc-session] ;; JDBC backed session store.
            [clojure.tools.reader.edn :as edn] ;; Safe de-serialization.
            [com.stuartsierra.component :as component])) ;; Life-cycle management.

(defrecord PGSessionStore [config db jdbc-store stopper]

  component/Lifecycle

  (start ;; Create the session store, and start the maintenance thread.
    [component]
    (let [jdbc-ss (ring-jdbc-session/make-session-store
                    (:datasource db) 
                    {:deserializer edn/read-string
                     :expire-secs (:max-session-age @config)})
          stopper (ring-jdbc-session/start-cleaner jdbc-ss)]
      (ring-jdbc-session/clean jdbc-ss) ;; Clean up old sessions on start.
      (assoc component
            :jdbc-store jdbc-ss
            :stopper stopper)))

  (stop [component] ;; Stop the maintenance thread.
    (when jdbc-store ;; started - stop it now.
      (ring-jdbc-session/clean jdbc-store) ;; Clean up old sessions on stop.
      (ring-jdbc-session/stop stopper))
    (dissoc component :jdbc-store :stopper))

  SessionStore ;; All members of this protocol are delegated to the JDBC session-store.

  (delete-session
    [store id]
    (delete-session jdbc-store id))

  (read-session
    [store id]
    (read-session jdbc-store id))

  (write-session
    [store id data]
    (write-session jdbc-store id data)))

(defn new-pg-session-store
  "Create a new session store record"
  ([] (new-pg-session-store nil nil))
  ([config] (new-pg-session-store config nil))
  ([config db] (map->PGSessionStore {:config config :db db})))

