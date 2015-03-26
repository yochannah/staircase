(ns staircase.test.projects
  (:import java.sql.SQLException)
  (:use clojure.test
        staircase.protocols
        staircase.helpers
        [clojure.tools.logging :only (info warn debug)]
        [staircase.config :only (db-options)]
        [com.stuartsierra.component :as component]
        [environ.core :only (env)])
  (:require staircase.sql
            staircase.resources
            [staircase.data :as data]
            [staircase.resources.projects :refer (make-trees new-projects-resource)]))

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

;; Some demo data

(def dummy-branches
  [
   {:parent_id nil :id 0 :title "Root"}
   {:parent_id 0 :id 1 :title "sub folder 1"}
   {:parent_id 1 :id 2 :title "sub-sub folder 1"}
   {:parent_id 0 :id 3 :title "sub folder 2"}
   {:parent_id 3 :id 4 :title "sub-sub folder 2"}
   {:parent_id 4 :id 5 :title "sub-sub-sub folder"}
   {:parent_id 0 :id 6 :title "empty child"}
   ])

(def dummy-leaves
  [
   {:project_id 0 :id "0.0"}
   {:project_id 0 :id "0.1"}
   {:project_id 1 :id "1.0"}
   {:project_id 1 :id "1.1"}
   {:project_id 1 :id "1.2"}
   {:project_id 2 :id "2.0"}
   {:project_id 3 :id "3.0"}
   {:project_id 3 :id "3.1"}
   {:project_id 3 :id "3.2"}
   {:project_id 4 :id "4.0"}
   {:project_id 4 :id "4.1"}
   {:project_id 5 :id "5.0"}
   ])

(def expected-tree
  {
   :id 0
   :title "Root"
   :type "Project"
   :contents [{:id "0.0"} {:id "0.1"}]
   :child_nodes [
                 {:id 1
                  :type "Project"
                  :title "sub folder 1"
                  :contents [{:id "1.0"} {:id "1.1"}, {:id "1.2"}]
                  :child_nodes [
                                {:id 2
                                 :type "Project"
                                 :title "sub-sub folder 1"
                                 :contents [{:id "2.0"}]
                                 :child_nodes []
                                 }]}
                 {:id 3
                  :type "Project"
                  :title "sub folder 2"
                  :contents [{:id "3.0"} {:id "3.1"}, {:id "3.2"}]
                  :child_nodes [
                                {:id 4
                                 :type "Project"
                                 :title "sub-sub folder 2"
                                 :contents [{:id "4.0"} {:id "4.1"}]
                                 :child_nodes [{:id 5
                                                :type "Project"
                                                :title "sub-sub-sub folder"
                                                :contents [{:id "5.0"}]
                                                :child_nodes []}]
                                 }]}
                 {:id 6
                  :type "Project"
                  :title "empty child"
                  :contents []
                  :child_nodes []}]})

;; And now, tests!

(defn get-projects []
  (new-projects-resource @db))

(deftest treeification
  (let [no-trees (make-trees [] [])]
    (is (= [] no-trees))) ;; No leaves and no branches means no trees.
  (let [trees (make-trees dummy-branches dummy-leaves)
        tree (first trees)]
    (is (= 1 (count trees)) "We only make one tree")
    (is (= expected-tree tree) "Which has the right form")))

(deftest write-to-projects
  (binding [staircase.resources/context {:user "nil@nil"}]
    (let [projects (get-projects)
          folder-id (create projects {:title "Test project"})
          retrieved (get-one projects folder-id)]
      (testing "Auto generated column values"
        (is (instance? java.util.UUID folder-id))
        (is (= folder-id (:id retrieved)))
        (is (instance? java.util.Date (:created_at retrieved)))
        (is (.after (now) (:created_at retrieved)) "Was created before now"))
      (testing "Existence"
        (is (exists? projects folder-id) "The new project exists"))
      (testing "Retrieved value"
        (is (= "Test project" (:title retrieved)))
        (is (= 0 (count (:contents retrieved))))
        (is (= 0 (count (:child_nodes retrieved)))))
      (testing "Adding item to folder"
        (let [item {
                    :item_id "some list name"
                    :item_type "List"
                    :source "NoMine"
                    }
              item-id (add-child projects folder-id item)
              updated-proj (get-one projects folder-id)]
          (is item-id "We get back an id")
          (is (= 1 (count (:contents updated-proj)))
              (str (pr-str updated-proj) " should have 1 item"))))
      (testing "Adding sub-project to folder"
        (let [subproject {:title "Sub project" :parent_id folder-id}
              sb-id (create projects subproject)
              updated-proj (get-one projects folder-id)]
          (is sb-id "We get back an id")
          (is (= 1 (count (:child_nodes updated-proj))))))
      )))

(def ids [:ID_A :ID_B :ID_C :ID_D])
(def times [:TIME_A :TIME_B :TIME_C])

;; Normalisation machinery - replace every time and id in the graph with
;; a number, making sure that we preserve identity and sequence between
;; items, ie. any two identical times should be given the same number and
;; earlier times should be given lower numbers.
(defn update-id
  [normed-ids thing]
  (update-in thing [:id] normed-ids))

(defn normalise-node
  [normed-times normed-ids project-graph]
  (-> (update-id normed-ids project-graph)
      (update-in [:created_at]    normed-times)
      (update-in [:last_accessed] normed-times)
      (update-in [:last_modified] normed-times)
      (update-in [:contents] #(into [] (map (partial update-id normed-ids) %)))
      (update-in [:child_nodes] #(into [] (map (partial normalise-node normed-times normed-ids) %)))))

(defn times-in-graph
  "Get all the times mentioned in the graph"
  [graph]
  (conj (mapcat times-in-graph (:child_nodes graph))
        (:created_at graph)
        (:last_accessed graph)
        (:last_modified graph)))

(defn ids-in-graph
  "Get all the ids mentioned in the graph"
  [graph]
  (concat [(:id graph)]
          (mapcat ids-in-graph (:child_nodes graph))
          (map :id (:contents graph))))

(defn normalise [project-graph]
  "Replace the variable things (ids and times) with sequential numbers, so we can compare"
  (when project-graph
    (let [graph-times (apply sorted-set (times-in-graph project-graph))
          graph-ids (ids-in-graph project-graph) ;; No point making set - they are UUIDs after all.
          normed-times (zipmap graph-times (range))
          normed-ids (zipmap graph-ids (range))]
      (normalise-node normed-times normed-ids project-graph))))

(def expected-nested-projects
  {
   :type "Project",
   :id 0,
   :title "root",
   :description nil,
   :created_at 0,
   :last_accessed 0,
   :last_modified 1,
   :contents [],
   :child_nodes
   [{
     :type "Project",
     :id 1,
     :title "sub folder",
     :description nil,
     :created_at 1,
     :last_accessed 1,
     :last_modified 2,
     :contents [],
     :child_nodes
     [{
       :type "Project",
       :title "sub sub folder",
       :description nil,
       :id 2,
       :created_at 2,
       :last_accessed 2,
       :last_modified 3,
       :child_nodes [],
       :contents
       [{:source "there",
         :item_type "thing",
         :item_id "thingy",
         :id 3}]
       }]
     }]})

(def expected-nested-projects-get-one ;; Get one sets access time.
  (assoc expected-nested-projects :last_accessed 4))

(def expected-nested-projects-b ;; Need to re-normalise due to the re-rooting.
  (-> (get-in expected-nested-projects [:child_nodes 0])
      normalise
      (assoc :last_accessed 3)))

(deftest writing-nested-projects
  (binding [staircase.resources/context {:user "nil@nil"}]
    (let [projects (get-projects)
          folder-a (create projects {:title "root"})
          _        (Thread/sleep 5) ;; We want predictable timestamps, so we nap between insertions.
          folder-b (add-child projects folder-a {:type "Project" :title "sub folder"})
          _        (Thread/sleep 5) ;; We want predictable times.
          folder-c (add-child projects folder-b {:type "Project" :title "sub sub folder"})
          _        (Thread/sleep 5) ;; We want predictable times.
          item-id  (add-child projects folder-c {:type "Item" :item_id "thingy" :item_type "thing" :source "there"})
          retrieved (get-all projects)]
      (testing "Correct nesting"
        (is (= 1 (count retrieved)) "There should only be one root project")
        (is (= expected-nested-projects (normalise (first retrieved)))))
      (testing "Correct nesting with get-one"
        (is (= expected-nested-projects-get-one (normalise (get-one projects folder-a)))))
      (testing "Accessing nested project"
        (is (= true (exists? projects folder-b)) "The sub folder exists")
        (is (= expected-nested-projects-b       (normalise (get-one projects folder-b)))))
      )))

