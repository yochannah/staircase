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
   :item_count 12
   :contents [{:id "0.0"} {:id "0.1"}]
   :child_nodes [
                 {:id 1
                  :type "Project"
                  :title "sub folder 1"
                  :item_count 4
                  :contents [{:id "1.0"} {:id "1.1"}, {:id "1.2"}]
                  :child_nodes [
                                {:id 2
                                 :type "Project"
                                 :title "sub-sub folder 1"
                                 :item_count 1
                                 :contents [{:id "2.0"}]
                                 :child_nodes []
                                 }]}
                 {:id 3
                  :type "Project"
                  :title "sub folder 2"
                  :item_count 6
                  :contents [{:id "3.0"} {:id "3.1"}, {:id "3.2"}]
                  :child_nodes [
                                {:id 4
                                 :type "Project"
                                 :title "sub-sub folder 2"
                                 :item_count 3
                                 :contents [{:id "4.0"} {:id "4.1"}]
                                 :child_nodes [{:id 5
                                                :type "Project"
                                                :title "sub-sub-sub folder"
                                                :item_count 1
                                                :contents [{:id "5.0"}]
                                                :child_nodes []}]
                                 }]}
                 {:id 6
                  :type "Project"
                  :title "empty child"
                  :item_count 0
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
                    :type "Item"
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

(deftest project-default-values
  (binding [staircase.resources/context {:user "nil@nil"}]
    (let [projects (get-projects)
          folder-id-a (create projects {})
          folder-id-b (create projects {})
          retrieved-a (get-one projects folder-id-a)
          retrieved-b (get-one projects folder-id-b)]
      (testing "A new title was generated for folder a"
        (is (= "new project 1" (:title retrieved-a))))
      (testing "And a different title was generated for folder b"
        (is (= "new project 2" (:title retrieved-b)))))))

;; Normalisation machinery - replace every time and id in the graph with
;; a number, making sure that we preserve identity and sequence between
;; items, ie. any two identical times should be given the same number and
;; earlier times should be given lower numbers.
(defn update-id
  [normed-ids thing]
  (update-in thing [:id] normed-ids))

(defn normalise-item
  [normed-times normed-ids item]
  (-> (update-id normed-ids item)
      (update-in [:created_at] normed-times)
      (update-in [:last_modified] normed-times)))

(defn normalise-node
  [normed-times normed-ids project-graph]
  (-> (update-id normed-ids project-graph)
      (update-in [:created_at]    normed-times)
      (update-in [:last_accessed] normed-times)
      (update-in [:last_modified] normed-times)
      (update-in [:contents] #(into [] (map (partial normalise-item normed-times normed-ids) %)))
      (update-in [:child_nodes] #(into [] (map (partial normalise-node normed-times normed-ids) %)))))

(defn times-in-item
  "Get all the times mentioned in an item"
  [item]
  (map #(% item) [:created_at :last_modified]))

(defn times-in-graph
  "Get all the times mentioned in the graph"
  [graph]
  (conj (concat (mapcat times-in-graph (:child_nodes graph))
                (mapcat times-in-item (:contents graph)))
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
   :item_count 2 ;; All items in graph, even though none at root.
   :contents [],
   :child_nodes
   [{
     :type "Project",
     :id 1,
     :title "sub folder",
     :description nil,
     :created_at 1,
     :last_accessed 1,
     :last_modified 3,
     :item_count 2 ;; This item, and item in sub sub project
     :contents [
                {:source "here"
                 :description nil
                 :last_modified 2
                 :created_at 2
                 :item_type "thing"
                 :item_id "thangy"
                 :id 4}],
     :child_nodes
     [{
       :type "Project",
       :title "sub sub folder",
       :description nil,
       :id 2,
       :created_at 3,
       :last_accessed 3,
       :last_modified 4,
       :item_count 1 ;; Just this item.
       :child_nodes [],
       :contents
       [{:source "there",
         :description nil
         :last_modified 4
         :created_at 4
         :item_type "thing",
         :item_id "thingy",
         :id 3}]
       }]
     }]})

(def expected-nested-projects-get-one ;; Get one sets access time.
  (assoc expected-nested-projects :last_accessed 5))

(def expected-nested-projects-b ;; Need to re-normalise due to the re-rooting.
  (-> (get-in expected-nested-projects [:child_nodes 0])
      normalise
      (assoc :last_accessed 4)))

(deftest ^:database writing-nested-projects
  (binding [staircase.resources/context {:user "nil@nil"}]
    (let [projects (get-projects)
          folder-a (create projects {:title "root"})
          _        (Thread/sleep 5) ;; We want predictable timestamps, so we nap between insertions.
          folder-b (add-child projects folder-a {:type "Project" :title "sub folder"})
          _        (Thread/sleep 5) ;; We want predictable times.
          item-id-a (add-child projects folder-b {:type "Item" :item_id "thangy" :item_type "thing" :source "here"})
          _        (Thread/sleep 5) ;; We really really want predictable times.
          folder-c (add-child projects folder-b {:type "Project" :title "sub sub folder"})
          _        (Thread/sleep 5) ;; We want predictable times, at this point a macro looks pretty good...
          item-id-b  (add-child projects folder-c {:type "Item" :item_id "thingy" :item_type "thing" :source "there"})
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

(deftest ^:database project-title-constraints
  (binding [staircase.resources/context {:user "nil@nil"}]
    (let [projects (get-projects)]
      (is (not (nil? (create projects {:title "name without slashes"}))) "Slashless names are fine")
      (is (= {:type :constraint-violation
              :constraint "projects_title_sluggish"}
             (try
               (create projects {:title "name/with/slashes"})
               :no-exception-thrown!
               (catch clojure.lang.ExceptionInfo e
                 (ex-data e))))))))

(deftest ^:database project-loop-constraints
  (binding [staircase.resources/context {:user "nil@nil"}]
    (let [projects (get-projects)
          proj-id  (create projects {})
          updated  (update projects proj-id {:parent_id proj-id})]
      (is (nil? (:parent_id updated))))))

(deftest ^:database identically-named-projects
  (binding [staircase.resources/context {:user "nil@nil"}]
    (let [foo      {:title "foo" :type "Project"}
          projects (get-projects)
          folder-a (create projects {:title "a"})
          folder-b (create projects {:title "b"})
          folder-foo (create projects foo)
          folder-foo-a (add-child projects folder-a foo)
          folder-foo-b (add-child projects folder-b foo)]
      (is (= 3 (count #{folder-foo folder-foo-a folder-foo-b})))
      (is (thrown? org.postgresql.util.PSQLException #"projects_owner_title_parent_id"
                   (add-child projects folder-a foo)))
      (is (thrown? org.postgresql.util.PSQLException #"projects_owner_title_parent_id"
                   (create projects foo))))))

