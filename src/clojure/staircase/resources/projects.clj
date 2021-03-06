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

(def tree-type "Project") ;; We call the things here "Projects"

;; Takes two maps and a root node (also a map), recursively
;; builds up the nested folder structure.
;; Elements that are not needed publicly are removed (eg: parent/project ids)
(defn treeify [branches leaves parent]
  "Construct a nested project from its flattened representation"
  (let [process (fn [m f] (->> (:id parent) (get m) (map f) vec))
        contents (process leaves #(dissoc % :project_id)) ;; Not needed publicly.
        child-nodes (process branches
                             (comp
                               #(dissoc % :parent_id) ;; They obviously belong to this node.
                               (partial treeify branches leaves)))
        item-count (apply + (count contents) (map :item_count child-nodes))]
    (assoc parent
           :item_count item-count ;; The number of items in this node and all sub-nodes.
           :child_nodes child-nodes ;; The treeified child nodes
           :contents contents ;; The processed leaves
           :type tree-type)))

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

(defn create-project
  "Create a project, returning map with created_at and id"
  [projects doc]
  (let [doc   (stringly-keyed doc)
        db    (:db projects)
        owner (:user res/context)
        title (or (get doc "title") ;; Given title, or generated one.
                  (->> (range) ;; infinite list of ints 0 ..
                       (map inc) ;; An infinite list starting from 1
                       (map #(str "new project "  %)) ;; Now its a list of names (TODO: make this configurable)
                       (filter (comp nil? (partial find-by-title projects))) ;; Find all the ones that haven't been assigned
                       first)) ;; Get the first unassigned generated name
        values (-> doc
                   (select-keys ["description" "parent_id"])
                   (update-in ["parent_id"] string->uuid)
                   (assoc "owner" owner "title" title))]
    (try
      (first (sql/insert! db :projects values))
      (catch org.postgresql.util.PSQLException e
        (if-let [[m con-name] (re-find #"violates check constraint \"(\w+)\"" (.getMessage e))]
          (throw (ex-info m {:type :constraint-violation
                             :constraint con-name}))
          (throw e))))))

(defmulti add-content
  "Add an item of content to a project, dispatching on the content's type"
  (fn [ps p c] (keyword (get c "type"))))

(defmethod add-content :Project
  [projects parent child]
  (create-project projects (assoc child "parent_id" parent)))

(defmethod add-content :Item
  [{db :db} project item]
  (first (sql/insert! db :project_contents
                   (-> item ;; White-list properties and link to project.
                       (select-keys ["item_id" "item_type" "source" "description"])
                       (assoc "project_id" (string->uuid project))))))

;; TODO: types (eg. Project) should be all lower-case, and probably be keywords.
(defn add-item-to-project
  "Either adds a child item or a child project"
  [projects project-id item]
  (when (exists? projects project-id) ;; Access control.
    (sql/with-db-transaction [trs (:db projects)]
      (let [trs-proj (assoc projects :db trs)
            {new-id :id mod-time :created_at} (add-content trs-proj project-id item)]
      ;; Touch the parent, setting its mod time to the creation time of its new child.
      (touch-project trs (string->uuid project-id) mod-time)
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

  (get-child [this id child-id]
    (first (find-item db (string->uuid id) (string->uuid child-id) (:user res/context))))

  (delete-child [this id child-id]
    (delete-item-from-project this id child-id))

  ;; TODO: add put-child, or update-child

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
