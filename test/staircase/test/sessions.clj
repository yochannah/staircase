;; While we mostly rely on a library for session storage, we have an obligation to test this because:
;;  a) We actually implement the protocol ourselves, albeit via delegation
;;  b) We have enough modifications to need checking
;;  c) We are responsible for creating the tables, so we need to check they work
;;  d) Sessions are a fundamental feature - we need to know they work as expected.
(ns staircase.test.sessions
  "Tests for the session store"
  (:use clojure.test
        staircase.protocols
        ring.middleware.session.store ;; provides read-session, write-session, delete-session
        staircase.helpers
        [clojure.tools.logging :only (info warn debug)]
        [staircase.config :only (db-options)]
        [com.stuartsierra.component :as component]
        [environ.core :only (env)]
        [staircase.sessions :as s])
  (:require
    staircase.sql
    [staircase.data :as data]
    [clj-time.core :as t]
    [clj-time.coerce :as tc]
    [clojure.java.jdbc :as sql]))

(def db-spec (db-options env))

;; Create a db, but do not start it - it is cycled for each test.
(def db (atom (data/new-pooled-db db-spec)))

(defn drop-tables [] 
  (debug "Dropping tables...")
  (staircase.sql/drop-all-tables db-spec))

;; Wrap tests in rounds of dropping the data-base to ensure a clean slate.
(defn clean-slate [f]
  (drop-tables)
  (f))

(defn clean-up [f]
  (try (f) (finally (drop-tables))))

(defn clean-slate [f]
  (drop-tables)
  (swap! db component/start)
  (f))

(defn clean-up [f]
  (try (f) (finally
             (swap! db component/stop)
             (drop-tables))))

(use-fixtures :each clean-slate clean-up)

(def two-minutes-ago (t/minus (t/now) (t/minutes 2)))

(def a-minute-ago (t/minus (t/now) (t/minutes 1)))

(def session-conf {:max-session-age 600})

(def dummy-data
  (map #(assoc %1 :session_key (str "fake:" (new-id)))
       [
        {:session_val "{:theory :phlogiston}"
         :session_ts (tc/to-sql-time (t/date-time 1774 8 1))}
        {:session_val "{:theory :geocentrism}"
         :session_ts (tc/to-sql-time (t/date-time 1543 3 1))}
        {:session_val "{:theory :efficient-market-hypothesis}"
         :session_ts (tc/to-sql-time two-minutes-ago)}
        {:session_val "{:theory :relativity}"
         :session_ts (tc/to-sql-time a-minute-ago)}]))

(defn insert-dummy-data [{db :db}]
  (apply sql/insert! db :ring_session dummy-data))

(defn all-sessions [{db :db}]
  (sql/query db ["select * from ring_session order by session_ts ASC"]))

(defn get-session-store []
  "Returns an initialised session store."
  (-> (s/new-pg-session-store (atom session-conf) @db)
      component/start))

(deftest test-empty-session-store
  (let [store (get-session-store)]
    (testing "bad keys"
      (testing "read"
        (is (= nil (read-session store "foo"))))
      (testing "delete"
        (is (= nil (delete-session store "bar"))))
      (testing "write"
        (is (thrown? java.lang.AssertionError (write-session store 123 nil)))))
    (testing "non-existent sessions"
      (testing "read" ;; Not found
        (is (= nil (read-session store (str (new-id))))))
      (testing "delete"
        (is (= nil (delete-session store (str (new-id))))))
      (testing "write"
        (let [data {:foo "bar"}
              id (write-session store nil data) ;; Write a new session.
              sessions (into [] (all-sessions store))
              timestamp (get-in sessions [0 :session_ts])
              sess (read-session store id)]
          (is id "There should be an id")
          (is (= (count sessions) 1)) ;; Just the one we inserted.
          (is timestamp (str "The sessions should have time stamps" (pr-str sessions))) ;; It has a best-before-date.
          (is (.before timestamp (now)) (str timestamp " should be earlier than now"))
          (is (= sess data) "The data was stored and retrieved correctly")
          (is (= (get-in sessions [0 :session_key]) id) "The returned key is the one stored in the db"))))))

(deftest test-pre-existing-sessions
  (let [store (get-session-store)]
    ;; Use the first store to bootstrap the DB.
    (insert-dummy-data store)
    ;; Reinitialise to test bootstrapping.
    (let [store (-> store
                    component/stop ;; Stop the old store
                    s/map->PGSessionStore ;; Records are maps.
                    component/start) ;; Start the new store.
          sessions (all-sessions store)]
      ;; Check the initial state.
      (testing "Cleaned up old sessions"
        (is (= 2 (count sessions))))
      (testing "Deleted the right sessions"
        (is (some #{"{:theory :relativity}"} (map :session_val sessions))))
      (testing "We can read pre-existing sessions"
        (let [sess (read-session store (:session_key (nth dummy-data 2)))]
          (is (= {:theory :efficient-market-hypothesis}  sess))))
      ;; Add a session a and make sure the expiry is updated.
      (let [sid (write-session store nil {:theory :oop})
            sessions (all-sessions store)]
        (is (= 3 (count sessions)))
        ;; Test update value
        (let [sid' (write-session store sid {:theory :foop})
              sess (read-session store sid)]
          (is (= sid sid'))
          (is (= sess {:theory :foop})))
        (is (= nil (delete-session store sid)))
        (let [sessions (all-sessions store)]
          (is (= 2 (count sessions))))))))

