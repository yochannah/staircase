(ns staircase.test.sessions
  (:import java.sql.SQLException)
  (:use clojure.test
        staircase.protocols
        ring.middleware.session.store
        staircase.helpers
        [clojure.tools.logging :only (info warn debug)]
        [staircase.config :only (db-options)]
        [com.stuartsierra.component :as component]
        [environ.core :only (env)]
        [staircase.sessions :as s])
  (:require
    [clj-time.core :as t]
    [clj-time.coerce :as tc]
    [clojure.java.jdbc :as sql]))

(def db-spec (db-options env))

(defn ignore-errors [f]
  (try (f) (catch SQLException e nil)))

(defn drop-tables [] 
  (debug "Dropping tables...")
  (doseq [table [:sessions]]
    (ignore-errors #(sql/db-do-commands db-spec (str "DELETE FROM " (name table)) (sql/drop-table-ddl table)))
    (debug "Dropped" table)))

(defn clean-slate [f]
  (drop-tables)
  (f))

(defn clean-up [f]
  (try (f) (finally (drop-tables))))

(use-fixtures :each clean-slate clean-up)

(def ten-years-from-now (t/plus (t/now) (t/years 10)))

(def thirty-years-from-now (t/plus (t/now) (t/years 30)))

(def dummy-data
  (map #(assoc %1 :id (new-id))
       [
        {:data "{:theory :phlogiston}"
         :valid_until (tc/to-sql-time (t/date-time 1774 8 1))}
        {:data "{:theory :geocentrism}"
         :valid_until (tc/to-sql-time (t/date-time 1543 3 1))}
        {:data "{:theory :efficient-market-hypothesis}"
         :valid_until (tc/to-sql-time ten-years-from-now)}
        {:data "{:theory :relativity}"
         :valid_until (tc/to-sql-time thirty-years-from-now)}]))

(defn insert-dummy-data [store]
    (apply sql/insert! (get-in store [:db :connection]) :sessions dummy-data))

(defn all-sessions [store]
  (into [] (sql/query (get-in store [:db :connection]) ["select * from sessions"])))

(deftest test-empty-session-store
  (let [store (-> (s/new-pg-session-store
                    {:max-session-age 60} {:connection db-spec})
                  component/start)]
    (testing "bad keys"
      (testing "read"
        (is (= {} (read-session store "foo"))))
      (testing "delete"
        (is (= nil (delete-session store "bar"))))
      (testing "write"
        (is (thrown? IllegalArgumentException (write-session store "quux" nil)))))
    (testing "non-existent sessions"
      (testing "read"
        (is (= {} (read-session store (new-id)))))
      (testing "delete"
        (is (= nil (delete-session store (new-id)))))
      (testing "write"
        (let [data {:foo "bar"}
              id (write-session store nil data)
              sessions (all-sessions store)
              bbd (get-in sessions [0 :valid_until])
              sess (read-session store id)]
          (is id "There should be an id")
          (is (= (count sessions) 1))
          (is bbd)
          (is (.before (now) bbd) (str bbd " should be after now"))
          (is (= (get-in sessions [0 :data]) (prn-str data)))
          (is (= (get-in sessions [0 :id]) id))
          (is (= sess data))
          (is (= (get (deref (:cache store)) id) data))
          (is (.before (now) (deref (:next-expiry store)))))))))

(defn same-time [a b fudge-factor]
  (let [interval (t/interval (t/minus a fudge-factor) (t/plus b fudge-factor))]
    (t/within? interval b)))

(def next-expiry (comp tc/from-sql-time deref :next-expiry))

(def one-min (t/seconds 60))

(def one-sec (t/seconds 1))

(deftest test-pre-existing-sessions
  (let [store (-> (s/new-pg-session-store
                    {:max-session-age 60} {:connection db-spec})
                  component/start)]
    ;; Use the first store to bootstrap the db.
    (insert-dummy-data store)
    ;; Reinitialise to test bootstrapping.
    (let [store (component/start (s/map->PGSessionStore store))
          sessions (all-sessions store)]
      ;; Check the initial state.
      (testing "Cleaned up old sessions"
        (is (= 2 (count sessions))))
      (testing "Deleted the right sessions"
        (is (some #{"{:theory :relativity}"} (map :data sessions))))
      (testing "Found the soonest expiring session"
        (is (same-time ten-years-from-now (next-expiry store) one-sec)))
      (testing "We can read pre-existing sessions"
        (let [sess (read-session store (:id (nth dummy-data 2)))]
          (is (= (:theory sess) :efficient-market-hypothesis))))
      ;; Add a session a and make sure the expiry is updated.
      (let [sid (write-session store nil {:theory :oop})
            sessions (all-sessions store)]
        (is (= 3 (count sessions)))
        (is (same-time (t/plus (t/now) one-min) (next-expiry store) one-sec))
        ;; Test update value
        (let [sid' (write-session store sid {:theory :foop})
              sess (read-session store sid)]
          (is (= sid sid'))
          (is (= sess {:theory :foop}))
          ;; Check that we don't make unnecessary db calls.
          (with-redefs [s/get-from-db #(throw (Exception. "Should not have accessed db"))]
            (let [sess' (read-session store sid)]
              (is (= sess' sess)))))
        (is (= nil (delete-session store sid)))
        (let [sessions (all-sessions store)]
          (is (= 2 (count sessions))))))))


