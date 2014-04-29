(ns staircase.test.histories
  (:import java.sql.SQLException)
  (:use clojure.test
        staircase.resources
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
  (doseq [table [:histories :steps :history_step]]
    (ignore-errors #(sql/db-do-commands db-spec (str "DELETE FROM " (name table)) (sql/drop-table-ddl table)))
    (debug "Dropped" table)))

(defn clean-slate [f]
  (drop-tables)
  (f))

(defn clean-up [f]
  (try (f) (finally (drop-tables))))

(use-fixtures :each clean-slate clean-up)

(deftest read-empty-histories
  (let [histories (new-history-resource :db {:connection db-spec})
        fake-id (new-id)]
    (binding [staircase.resources/context {:user "foo@bar.org"}]
      (try
        (component/start histories)
        (catch SQLException e
          (warn "Bad connection details" (prn-str db-spec))
          (throw (Exception. "Could not initialise resource" e))))
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
    (let [histories (component/start (new-history-resource :db {:connection db-spec}))
          new-id (create histories {:title "My new history" :description "A testing history"})
          got (get-one histories new-id)
          hists (get-all histories)]
      (testing "Return value of create"
        (is (instance? java.util.UUID new-id)))
      (testing "retrieved record"
        (is (= got {:owner "quux@bar.org" :steps [] :id new-id :title "My new history" :description "A testing history"})))
      (testing "The new state of the world - created history in hists"
        (is (some #{new-id} (map :id hists))))
      (testing "The numbers of histories"
        (is (= 1 (count hists))))
      (testing "The existence of the new history"
        (is (exists? histories new-id)))
      (let [updated (update histories new-id {:title "changed the title"})
            retrieved (get-one histories new-id)
            hists (get-all histories)]
        (testing "Changed the title"
          (is (= "changed the title" (:title updated)))
          (is (= "changed the title" (:title retrieved))))
        (testing "Changed, and did not add a history"
          (is (= 1 (count hists)))
          (is (= {:steps 0 :owner "quux@bar.org" :title "changed the title" :description "A testing history" :id new-id} (hists 0))))))))

