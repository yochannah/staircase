(ns staircase.projects
  (:import java.sql.SQLException)
  (:import [java.sql SQLException])
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
    [clojure.zip :as z]
    [clojure.string :as str]
    [clj-time.core :as t]
    [clj-time.coerce :as tc]
    [cheshire.core :as json :refer :all]
    [clojure.java.jdbc :as sql]))

(def db {:connection (db-options env)})
(def db-spec (db-options env))


(defn sql-get-all-projects []
  (sql/query db-spec [(str "select * from projects")]))

(defn sql-update-project [id data]
  (try

  (sql/with-db-transaction [db db-spec]
  (sql/update! db-spec :projects
    {:title (data "title") :description (data "description")}
    ["id = ?" (read-string id)]))

  (catch Exception e
    (.getNextException e))))

(defn sql-delete-project [id]
  (sql/delete! db-spec :projects ["id = ?" id]))

(defn sql-delete-item [id]
  (sql/delete! db-spec :project_contents ["id = ?" id]))

(defn sql-get-project-items []
  (sql/query db-spec [(str "select * from project_contents")]))


(defn sql-get-a-project [id]
  (sql/query db-spec ["select * from projects where id = ?" id]))

(defn sql-get-children-projects [id]
  (sql/query db-spec ["select * from projects where parent_id = ?" id]))

(defn sql-get-single-project [id]
  (sql/query db-spec ["select * from projects where id = ?" id]))

(defn sql-create-project [data]
  (sql/insert! db-spec :projects
    {:parent_id (data "parent_id") :title (data "title") :owner_id "josh" :description "Generic Description" :last_modified (staircase.helpers/sql-now) :last_accessed (staircase.helpers/sql-now) :created (staircase.helpers/sql-now) }))

(defn sql-add-item-to-project [pid item]
  (sql/insert! db-spec :project_contents item))

(defn project-with-contents [p]
  (assoc p :contents (sql-get-single-project (:id p))
          :children (map (fn [x] (assoc x :type "Project")) (sql-get-children-projects (:id p)))))

(defn make-tree-orig
   ([coll]
      (let [root (first (remove :parent_id coll))]
               {:node root :children (make-tree-orig root coll)}))

   ([root coll]
       (for [x coll :when (= (:parent_id x) (:id root))]
           {:node x :children (make-tree-orig x coll)})))

(defn make-tree-orig
   ([root coll]
       (for [x coll]
            (cond
              (= (:parent x) (:id root)) {:node root :children (make-tree-orig x coll)}
              (not= (:parent x) (:id root)) {:node root :status "childless"}))))

(defn make-tree-list [coll]
  (for [nxt coll]
    {:node coll}))

(defn childrenfilter[i]
  (if (= (i :parent_id) 47) true false))

(defn create-zipper [s]
  (let [g (group-by :parent_id s)] 
    (z/zipper g #(map :id (g %)) nil (-> nil g first :id))))

(defn treeify [mapping allitems parent]
  (let [pid (:id parent)
        myitems (get allitems (:id parent))
        kids (get mapping pid)
        with-gks (map (fn [r] (treeify mapping allitems r)) kids)]
    (assoc parent :child_nodes with-gks :contents myitems :type "Project")))


(defn maketree [res allitems]
  (let [kids-of (group-by :parent_id res)
        everything (group-by :project_id allitems)
        roots (get kids-of nil)]
    (map (fn [r] (treeify kids-of everything r)) roots)))

(defn get-all-projects []
  (let [results (sql-get-all-projects)
      allitems (sql-get-project-items)]
    (json/generate-string (hash-map :title "All Projects" :child_nodes (maketree results allitems)))))

(defn get-single-project [id]
  (let [results (sql-get-single-project (read-string id))
      allitems (sql-get-project-items)]
    (json/generate-string (hash-map :title "All Projects" :child_nodes (maketree results allitems)))))

(defn delete-project [id]
  (let [results (sql-delete-project (read-string id))]
    (json/generate-string results)))

(defn delete-item [id]
  (let [results (sql-delete-item (java.util.UUID/fromString id))]
    (json/generate-string results)))

(defn update-project [id payload]
  (let [results (sql-update-project id payload)]
    (json/generate-string results)))

(defn add-item-to-project [pid payload]
  (def results (sql-add-item-to-project pid (assoc payload "id" (new-id))))
  (json/generate-string {:success true} {:pretty true}))

(defn create-project [payload]
  (sql-create-project payload))