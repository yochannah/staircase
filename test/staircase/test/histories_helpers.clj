(ns staircase.test.histories-helpers
  (:use clojure.test
        staircase.protocols
        [clojure.tools.logging :only (info warn debug)]
        [staircase.config :only (db-options)]
        [com.stuartsierra.component :as component]
        [clojure.tools.logging :only (info warn debug)]
        [environ.core :only (env)])
  (:require staircase.resources
            [staircase.sql :as sql]
            [staircase.resources.histories :refer (new-history-resource
                                                   get-steps-of
                                                   get-history-steps-of
                                                   add-all-steps)]
            [staircase.resources.steps :refer (new-steps-resource)]
            [staircase.data :as data]))

;; The next 20 lines are a bit boiler-platey, and could be extracted.
(def db-spec (db-options env))

;; Create a db, but do not start it - it is cycled for each test.
(def db (atom (data/new-pooled-db db-spec)))

;; We set up and tear down by deleting everything in the database.
(defn drop-tables [] (sql/drop-all-tables db-spec))

(def ^:dynamic id-a nil)
(def ^:dynamic id-b nil)

(def context {:user "no-one@no-where.nil"})

(defn load-data [f]
  (let [histories (new-history-resource @db)
        steps (new-steps-resource @db)]
    ;; Two separate binding sets are needed, as the context must be bound before
    ;; calls to create, whereas normally binding are not visible to each other.
    (binding [staircase.resources/context context]
      (binding [id-a (create histories {:title "My new history A" :description "has a description"})
                id-b (create histories {:title "My new history B" :description nil})]
        (create steps {:title "foo" :tool "tool-1" :history_id id-a})
        (create steps {:title "bar" :tool "tool-2" :history_id id-a})
        (create steps {:title "baz" :tool "tool-3" :history_id id-a})
        (create steps {:title "can't be found" :tool "tool-1"
                       :history_id (create histories {:title "History C"})})
        (f)))))

(defn run-test [f]
  (drop-tables)
  (swap! db component/start)
  (try (load-data f)
       (finally
         (swap! db component/stop)
         (drop-tables))))

(use-fixtures :each run-test)

;; Our tests:

(deftest test-get-steps-of
  (let [resource {:db @db}]
    (let [a-steps (get-steps-of resource id-a)]
      (testing "history A has two steps"
        (is (= 3 (count a-steps))))
      (testing "steps come out oldest -> newest"
        (is (= (list "foo" "bar" "baz") (map :title a-steps)))))
    (let [b-steps (get-steps-of resource id-b)]
      (testing "we only get steps of the history we asked for"
        (is (empty? b-steps))))))

(deftest test-get-history-steps-of
  (let [resource {:db @db}
        links (get-history-steps-of resource id-a 2)]
    (is (= 2 (count links)))
    (is (= id-a (:history_id (first links))))))

(deftest test-add-all-steps
  (let [resource {:db @db}
        links (get-history-steps-of resource id-a 2)]
    (add-all-steps resource id-b links)
    (let [steps (get-steps-of resource id-b)]
      (is (= 2 (count steps)))
      (is (= (list "foo" "bar") (map :title steps))))))

(deftest test-get-steps-of-security
  (let [resource {:db @db}]
    (testing "only the owner can access the histories, even if the id is leaked"
      (binding [staircase.resources/context context]
        (let [a-steps (get-steps-of resource id-a)]
          (is (= 3 (count a-steps)))))
      (binding [staircase.resources/context {:user "blackhat@pirates.r.us"}]
        (let [a-steps (get-steps-of resource id-a)]
          (is (empty? a-steps)))))))
