(ns staircase.resources.projects
  (:use staircase.protocols
        staircase.helpers
        [clojure.tools.logging :only (info)])
  (:import java.sql.SQLException)
  (:require staircase.sql
            clojure.string
            [yesql.core :refer (defquery)]
            [staircase.resources :as res]
            [honeysql.helpers :refer (select from where)]
            [honeysql.core :as hsql]
            [cheshire.core :as json]
            [clojure.java.jdbc :as sql]))

(defn find-by-title [projects title]
  (first (get-where projects [:= :title title])))

(defquery get-project-items
  "db/queries/get-project-items.sql")

(defquery get-project-and-subprojects
  "db/queries/get-project-and-subprojects.sql")

(defquery get-items-of-project
  "db/queries/get-items-of-project.sql")

(defquery find-item
  "db/queries/find-project-content.sql")

(defn item-exists? [db project item]
  (and project
       item
       (first (find-item db project (:user res/context) item))))

;; Takes two maps and a root node (also a map), recursively
;; builds up the nested folder structure.
(defn treeify [branches leaves parent]
  (let [pid (:id parent)
        contents (get leaves pid)
        child-nodes (->> (get branches pid)
                         (map (partial treeify branches leaves)))]
    (-> parent
        (dissoc :parent_id) ;; not needed publicly - remove.
        (assoc
           :child_nodes child-nodes
           :contents contents
           :type "Project"))))

;; Finds the roots (branches without parents), and then
;; tree-ifies them.
(defn make-trees [branches leaves]
  (let [branches-of (group-by :parent_id branches)
        leaves-of (group-by :project_id leaves)
        as-tree (partial treeify branches-of leaves-of)
        roots (get branches-of nil)]
    (map as-tree roots)))

(defn add-item-to-project [projects project-id item]
  (when (exists? projects project-id) ;; Access control.
    (sql/insert! (:db projects) :project_contents
                (-> item
                  (select-keys ["item_id" "item_type" "source"])
                  (assoc "project_id" project-id)))))

(defn delete-item-from-project [{db :db} project-id item-id]
  (let [pid (string->uuid project-id)
        iid (string->uuid item-id)]
    (when (item-exists? db pid iid))
      (sql/delete! db :project_contents ["id = ?" iid])
      iid))

(defrecord Projects [db]

  Resource

  (get-all [_] ;; Should we be recording access here?
    (let [branches (sql/query db
                              (res/all-belonging-to :projects))
          leaves (get-project-items db (:user res/context))
          tree (make-tree branches leaves)]
      (vector (make-trees branches leaves))))
  
  (get-one [_ id] ;; there can only be one, and first is nil safe.
    (let [uuid (string->uuid id)
          user (:user res/context)
          now (sql-now)
          leaves (get-items-of-project db uuid user)
          branches (get-project-and-subprojects db uuid user)]
      (when-let [proj (first (make-trees branches leaves))]
        ;; record that we accessed this project.
        ;; if this becomes a bottleneck then it should be moved
        ;; into a messaging queue.
        (sql/update! db
                     :projects
                     {:last_accessed now}
                     ["id=?" (:id proj)])
        ;; Return the project, with its new access time.
        (assoc proj :last_accessed now))))

  (update [_ id doc]
    (staircase.sql/update-owned-entity db :projects
      (assoc res/context :id id)
      (doc -> ;; Only allow title and description to be updated.
           (select-keys ["title" "description"])
           (assoc :last_modified (sql-now)))))

  (delete [_ id] ;; Deletion of subprojects and items is handled
                 ;; by cascading deletions.
    (staircase.sql/delete-entity db :projects id))

  (create [_ doc]
    (let [owner (:user res/context)
          title (or (:title doc) ;; Given title, or generated one.
                    (->> (range) ;; infinite list of ints 0 ..
                         (map #(str "new project " %))
                         (filter (comp nil? find-by-title))
                         first))
          values (-> doc
                     (select-keys ["description" "parent_id"])
                     (assoc "owner" owner "title" title))]
      (first (sql/insert! db :projects values))))

  (exists? [_ id]
    (staircase.sql/exists-with-owner
      db
      :projects
      (assoc res/context :id id)))

  Searchable

  (get-where [_ constraint]
    (sql/query db
               (hsql/format {:select [:*]
                             :from [:projects]
                             :where [:and constraint
                                          [:= :owner :?user]]}
                            :params res/context)
               :result-set-fn vec))
  
  )

(defn new-projects-resource [db] (map->ProjectsResource {:db db}))
