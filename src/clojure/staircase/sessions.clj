(ns staircase.sessions
  (use ring.middleware.session.store
        [clojure.tools.logging :only (info warn debug)]
        [staircase.helpers :only (string->uuid new-id)]
        [clojure.algo.monads :only (domonad maybe-m)])
  (:require staircase.sql
            [clojure.tools.reader.edn :as edn]
            [com.stuartsierra.component :as component]
            [clojure.java.jdbc :as sql]))

(defn- now [] (java.sql.Timestamp. (System/currentTimeMillis)))

(defn get-expiry [duration-secs]
  (java.sql.Timestamp. (+ (System/currentTimeMillis) (* duration-secs 1e3))))

;; TODO - periodically clean out old sessions.
;; use a background thread?
(defn clear-old-sessions [db-spec]
  (sql/delete! db-spec :sessions ["valid_until <= ?" (now)]))

(defn- find-next-expiry [db-spec]
  (sql/query db-spec
              ["select valid_until
                 from sessions
                 where valid_until > ?
                 order by valid_until asc
                 limit 1" (now)]
              :result-set-fn (comp :valid_until first)))

;; Get from the cache, provided the next expiry hasn't arrived.
(defn- get-from-cache [{:keys [cache next-expiry]} id]
  (when-let [next-expiry (deref next-expiry)]
    (when (.before (now) next-expiry) (get-in @cache [id]))))

(defn get-from-db
  [store id]
  (let [db (:db store)
        cache (:cache store)]
    (debug "Reading session from db:" id)
    (domonad maybe-m ;; Look in persistent store.
             [uuid (string->uuid id)
              data (sql/query ;; nil-safe - returns nil if none found.
                      db
                      ["select data from sessions where id = ? and valid_until > ?" uuid (now)]
                      :result-set-fn (comp :data first))]
             (try
               (let [session (edn/read-string data)]
                 (swap! cache assoc id session) ;; Cache for future retrieval
                 session)
               (catch Exception e
                 (warn "Invalid data:" data e)
                 (delete-session store uuid))))))

(def write-monitor (Object.))

(defrecord PGSessionStore [config db cache next-expiry]

  component/Lifecycle

  (start
    [component]
    ;; Clean up old expired sessions.
    (clear-old-sessions db)
    ;; Set-up in-memory caching.
    (assoc component
           :next-expiry (atom (find-next-expiry db))
           :cache (atom {})))

  (stop [component] (dissoc component :next-expiry :cache))

  SessionStore

  (delete-session
    [store id]
    (swap! cache dissoc id) ;; Evict from cache.
    (when-let [uuid (string->uuid id)]
      ;; Delete this session.
      (locking write-monitor
        (sql/delete! db :sessions ["id = ?" uuid]))
      ;; Use this db-access opportunity to clean out the db a little.
      (clear-old-sessions db)
      nil))

  (read-session
    [store id]
    (or
      (get-from-cache store id) ;; Get from in-memory cache, if available.
      (get-from-db store id) ;; Retrieve from DB.
      {})) ;; As per the interface, return empty map if not in db.

  (write-session
    [store id data]
    (if-let [uuid (string->uuid (or id (new-id)))]
      (let [valid-until (get-expiry (:max-session-age @config))
            session {:data (prn-str data) :valid_until valid-until}
            where-clause ["id=?" uuid]]
        (debug "Storing" data "until" valid-until)
        (locking write-monitor
          (if (get-from-db store uuid)
            (sql/update! db
                        :sessions
                        session
                        where-clause)
            (sql/with-db-transaction [t-con db]
              (sql/delete! t-con ;; Might be a lingering expired session in there.
                          :sessions
                          where-clause)
              (sql/insert! t-con
                          :sessions
                          (assoc session :id uuid)))))
        (swap! next-expiry #(if (and %1 (.before %1 %2)) %1 %2) valid-until)
        (swap! cache dissoc id) ;; Evict from cache.
        uuid)
      (throw (IllegalArgumentException. (str id " is not a good key"))))))

(defn new-pg-session-store
  ([] (new-pg-session-store nil nil))
  ([config] (new-pg-session-store config nil))
  ([config db] (map->PGSessionStore {:config config :db db})))

