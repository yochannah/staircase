(ns staircase.test.data
  (:import java.sql.SQLException)
  (:use clojure.test
        staircase.data
        staircase.protocols
        staircase.resources.histories
        staircase.resources.steps
        [clojure.tools.logging :only (info warn debug)]
        [staircase.config :only (db-options)]
        [com.stuartsierra.component :as component]
        [clojure.tools.logging :only (info warn debug)]
        [environ.core :only (env)])
  (:require staircase.resources
            [clojure.java.jdbc :as sql]))

(def db {:connection (db-options env)})

(defn ignore-errors [f]
  (try (f) (catch SQLException e nil)))

(defn drop-tables [] 
  (debug "Dropping tables...")
  (doseq [table [:histories :steps :history_step]]
    (ignore-errors #(sql/db-do-commands (:connection db)
                                        (str "DELETE FROM " (name table))
                                        (sql/drop-table-ddl table)))
    (debug "Dropped" table)))

(def ^:dynamic id-a nil)
(def ^:dynamic id-b nil)

(def context {:user "no-one@no-where.nil"})

(defn init [f]
  (let [histories (component/start (new-history-resource :db db))
        steps (component/start (new-steps-resource :db db))]
    ;; Two separate binding sets are needed, as the context must be bound before
    ;; calls to create.
    (binding [staircase.resources/context context]
      (binding [id-a (create histories {:title "My new history A" :description "has a description"})
                id-b (create histories {:title "My new history B" :description nil})]
        (create steps {:title "foo" :history_id id-a})
        (create steps {:title "bar" :history_id id-a})
        (create steps {:title "baz" :history_id id-a})
        (create steps {:title "can't be found"
                       :history_id (create histories {:title "History C"})})
        (f)))))

(defn clean-slate [f]
  (drop-tables)
  (f))

(defn clean-up [f]
  (try (f) (finally (drop-tables))))

(use-fixtures :each clean-slate init clean-up)

(deftest test-get-steps-of
  (let [a-steps (get-steps-of {:db db} id-a)]
    (testing "history A has two steps"
      (is (= 3 (count a-steps))))
    (testing "steps come out oldest -> newest"
      (is (= (list "foo" "bar" "baz") (map :title a-steps)))))
  (let [b-steps (get-steps-of {:db db} id-b)]
    (testing "we only get steps of the history we asked for"
      (is (empty? b-steps)))))

(deftest test-get-history-steps-of
  (let [links (get-history-steps-of {:db db} id-a 2)]
    (is (= 2 (count links)))
    (is (= id-a (:history_id (first links))))))

(deftest test-add-all-steps
  (let [links (get-history-steps-of {:db db} id-a 2)]
    (add-all-steps {:db db} id-b links)
    (let [steps (get-steps-of {:db db} id-b)]
      (is (= 2 (count steps)))
      (is (= (list "foo" "bar") (map :title steps))))))

(deftest test-get-steps-of-security
  (testing "only the owner can access the histories, even if the id is leaked"
    (binding [staircase.resources/context context]
      (let [a-steps (get-steps-of {:db db} id-a)]
        (is (= 3 (count a-steps)))))
    (binding [staircase.resources/context {:user "blackhat@pirates.r.us"}]
      (let [a-steps (get-steps-of {:db db} id-a)]
        (is (empty? a-steps))))))
