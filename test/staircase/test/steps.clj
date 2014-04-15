(ns staircase.test.steps
  (:import java.sql.SQLException)
  (:use clojure.test
        staircase.resources
        staircase.resources.steps
        staircase.resources.histories
        staircase.protocols
        staircase.helpers
        [clojure.tools.logging :only (info warn debug)]
        [staircase.config :only (db-options)]
        [com.stuartsierra.component :as component]
        [environ.core :only (env)])
  (:require [clojure.java.jdbc :as sql]))

(def db-spec (db-options env))

(defn ignore-errors [f]
  (try (f) (catch SQLException e nil)))

(defn drop-tables [] 
  (debug "Dropping tables...")
  (doseq [table [:histories :steps :step_step]]
    (ignore-errors #(sql/db-do-commands db-spec (str "DELETE FROM " (name table)) (sql/drop-table-ddl table)))
    (debug "Dropped" table)))

(defn clean-slate [f]
  (drop-tables)
  (f))

(defn clean-up [f]
  (try (f) (finally (drop-tables))))

(use-fixtures :each clean-slate clean-up)

(deftest read-empty-steps
  (let [steps (component/start (new-steps-resource :db {:connection db-spec}))
        fake-id (new-id)]
    (testing "get-all"
      (is (= [] (get-all steps))))
    (testing "exists?"
      (is (not (exists? steps fake-id))))
    (testing "get-one"
      (is (= nil (get-one steps fake-id))))
    (testing "delete"
      (is (= nil (delete steps fake-id))))
    (testing "update"
      (is (= nil (update steps fake-id {:title "bar"}))))))

(deftest write-to-empty-steps
  (binding [staircase.resources/context {:user "no-one@no-where.nil"}]
    (let [db {:connection db-spec}
          steps (component/start (new-steps-resource :db db))
          histories (component/start (new-history-resource :db db))
          my-history (create histories {:title "test history"})
          doc-1 {"history_id" my-history :title "step 1" :tool "http://tools.intermine.org/quicksearch"}
          doc-2 {"history_id" my-history :title "step 2" :tool "http://tools.intermine.org/resultstable"}
          id-1 (create steps doc-1)
          id-2 (create steps doc-2)
          got (get-one steps id-1)
          all (get-all steps)]
      (testing "Return value of create"
        (is (instance? java.util.UUID id-1)))
      (testing "retrieved record"
        (is (= (dissoc got :created_at) (-> doc-1 (assoc :id id-1 :data nil) (dissoc "history_id")))))
      (testing "The new state of the world - created step in all steps"
        (is (some #{id-1} (map :id all))))
      (testing "The numbers of steps"
        (is (= 2 (count all))))
      (testing "The existence of the new steps"
        (is (exists? steps id-1))
        (is (exists? steps id-2)))
      (testing "That the step was added to its history"
        (let [history (get-one histories my-history)]
          (is (= 2    (count (:steps history))))
          (is (= id-2 (get-in history [:steps 0]))))) ;; Steps should be listed newest first.
      (let [updated (update steps id-1 {:title "changed the title"})
            retrieved (get-one steps id-1)
            all (get-all steps)]
        (testing "Still in history"
          (is (some #{id-1} (:steps (get-one histories my-history)))))
        (testing "Changed the title"
          (is (= "changed the title" (:title updated)))
          (is (= "changed the title" (:title retrieved))))
        (testing "Changed, and did not add a step"
          (is (= 2 (count all))))))))

