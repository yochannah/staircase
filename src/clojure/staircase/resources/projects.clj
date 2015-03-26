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
        contents (map #(dissoc % :project_id) (get leaves pid)) ;; Not needed publicly.
        child-nodes (->> (get branches pid)
                         (map (partial treeify branches leaves)))]
    (-> parent
        (dissoc :parent_id) ;; not needed publicly - remove.
        (assoc
           :child_nodes (vec child-nodes)
           :contents (vec contents)
           :type "Project"))))

;; Finds the roots (branches without parents), and then
;; tree-ifies them.
(defn make-trees [branches leaves & [target-id]] ;; When processing true roots, target-id is nil.
  (let [branches-of (group-by :parent_id branches)
        leaves-of (group-by :project_id leaves)
        as-tree (partial treeify branches-of leaves-of)
        root-id (when target-id ;; The when is unnecessary, but saves a pointless loop on get-all
                  (->> branches-of
                       (filter (fn [[k nodes]] (some #{target-id} (map :id nodes))))
                       (map first)
                       first))
        roots (get branches-of root-id)]
    (map as-tree roots)))

(defn touch-project
  "Updates the last modified time"
  [db project-id & [mod-time]]
  (sql/update! db
               :projects
               {:last_modified (or mod-time (sql-now))}
               ["id=?" project-id]))

(defn add-content-to-project
  "Add a piece of content to a project, returning map with created_at and id"
  [db project-id item]
  (first (sql/insert! db :project_contents
                   (-> item ;; White-list properties and link to project.
                       (select-keys ["item_id" "item_type" "source"])
                       (assoc "project_id" project-id)))))

(defn create-project
  "Create a project, returning map with created_at and id"
  [projects doc]
  (let [doc   (stringly-keyed doc)
        db    (:db projects)
        owner (:user res/context)
        title (or (get doc "title") ;; Given title, or generated one.
                  (->> (range) ;; infinite list of ints 0 ..
                       (map #(str "new project " %))
                       (filter (comp nil? (partial find-by-title projects)))
                       first))
        values (-> doc
                   (select-keys ["description" "parent_id"])
                   (assoc "owner" owner "title" title))]
    (first (sql/insert! db :projects values))))

(defn add-subproject
  "Add a sub-project to a project, returning map with created_at and id"
  [projects parent child]
  (create-project projects (assoc child "parent_id" parent)))

;; TODO: types (eg. Project) should be all lower-case
(defn add-item-to-project
  "Either adds a child item or a child project"
  [projects project-id item]
  (when (exists? projects project-id) ;; Access control.
    (sql/with-db-transaction [trs (:db projects)]
      (let [trs-proj (assoc projects :db trs)
            is-subproject? (= "Project" (get item "type"))
            {new-id :id mod-time :created_at} (if is-subproject?
                     (add-subproject trs-proj project-id item)
                     (add-content-to-project trs project-id item))]
      ;; Touch the parent, setting its mod time to the creation time of its new child.
      (touch-project trs project-id mod-time)
      new-id))))

(defn delete-item-from-project [{db :db} project-id item-id]
  (let [pid (string->uuid project-id)
        iid (string->uuid item-id)]
    (when (item-exists? db pid iid))
      (sql/delete! db :project_contents ["id = ?" iid])
      iid))

(defrecord ProjectsResource [db]

  Resource

  (get-all [_] ;; We don't mark projects accessed when we get all of them.
    (let [branches (map #(dissoc % :owner) ;; We don't need to expose owner - it might be useful later though.
                        (sql/query db (res/all-belonging-to :projects)))
          leaves (get-project-items db (:user res/context))
          trees (make-trees branches leaves)]
      (vec trees)))
  
  (get-one [_ id]
    """
    Get the project with the given id, or nil
    ----------------
    Respects access restrictions in regards to the current user.
    """
    (let [uuid (string->uuid id)
          user (:user res/context)
          now (sql-now)
          leaves   (get-items-of-project db uuid user)
          branches (get-project-and-subprojects db uuid user)]
      (when-let [proj (first (make-trees branches leaves uuid))]
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
      (-> doc ;; Only allow title and description to be updated.
          (select-keys ["title" "description"])
          (assoc :last_modified (sql-now))))) ;; touch the document.

  (delete [_ id] ;; Deletion of subprojects and items is handled
                 ;; by cascading deletions.
    (staircase.sql/delete-entity db :projects id))

  (create [this doc]
    (:id (create-project this doc)))

  (exists? [_ id]
    (staircase.sql/exists-with-owner
      db
      :projects
      (assoc res/context :id id)))

  SubindexedResource

  (delete-child [this id child-id]
    (delete-item-from-project this id child-id))

  (add-child [this id child] ;; Make sure all the keys are strings in the child
    (add-item-to-project this id (stringly-keyed child)))

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
