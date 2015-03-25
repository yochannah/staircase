(ns staircase.test.histories
  (:use clojure.test
        staircase.resources
        staircase.resources.histories
        staircase.protocols
        staircase.helpers
        [clojure.tools.logging :only (info warn debug)]
        [staircase.config :only (db-options)]
        [com.stuartsierra.component :as component]
        [environ.core :only (env)])
  (:require staircase.sql
            [staircase.data :as data]
            [clojure.java.jdbc :as sql]))

;; The next 20 lines are a bit boiler-platey, and could be extracted.
(def db-spec (db-options env))

;; Create a db, but do not start it - it is cycled for each test.
(def db (atom (data/new-pooled-db db-spec)))

;; We set up and tear down by deleting everything in the database.
(defn drop-tables [] 
  (debug "Dropping tables...")
  (staircase.sql/drop-all-tables db-spec))

(defn clean-slate [f]
  (drop-tables)
  (swap! db component/start)
  (f))

(defn clean-up [f]
  (try (f) (finally
             (swap! db component/stop)
             (drop-tables))))

(use-fixtures :each clean-slate clean-up)

;; Should only be called within a fixure.
(defn get-histories [] (new-history-resource @db))

;; End of boilerplate - tests!

(deftest read-empty-histories
  (let [histories (get-histories)
        fake-id (new-id)]
    (binding [staircase.resources/context {:user "foo@bar.org"}]
      (testing "get-all"
        (is (= [] (get-all histories))))
      (testing "exists?"
        (is (not (exists? histories fake-id))))
      (testing "get-one"
        (is (= nil (get-one histories fake-id))))
      (testing "delete"
        (is (= nil (delete histories fake-id))))
      (testing "update"
        (is (= nil (update histories fake-id {:title "bar"})))))))

(deftest write-to-empty-histories
  (binding [staircase.resources/context {:user "quux@bar.org"}]
    (let [histories (get-histories)
          new-id (create histories {:title "My new history" :description "A testing history"})
          got (get-one histories new-id)
          hists (get-all histories)]
      (testing "generated column values"
        (is (instance? java.util.UUID new-id))
        (is (instance? java.util.Date (:created_at got))))
      (testing "retrieved record"
        (let [expected {:owner "quux@bar.org"
                        :steps []
                        :id new-id
                        :title "My new history"
                        :description "A testing history"
                        :created_at (:created_at got)}]
        (is (= got expected))))
      (testing "The new state of the world - created history in hists"
        (is (some #{new-id} (map :id hists))))
      (testing "The numbers of histories"
        (is (= 1 (count hists))))
      (testing "The existence of the new history"
        (is (exists? histories new-id)))
      (let [updated (update histories new-id {:title "changed the title" :created_at "sneaky attempt to change the past"})
            retrieved (get-one histories new-id)
            hists (get-all histories)]
        (testing "Changed the title"
          (is (= "changed the title" (:title updated)))
          (is (= (:created_at got) (:created_at updated))) ;; Cannot change the creation date.
          (is (= "changed the title" (:title retrieved))))
        (testing "Changed, and did not add a history"
          (let [retrieved (hists 0)
                expected {:steps 0
                          :owner "quux@bar.org"
                          :title "changed the title"
                          :description "A testing history"
                          :id new-id
                          :created_at (:created_at retrieved)}]
            (is (= 1 (count hists)))
            (is (= expected retrieved))))))))

