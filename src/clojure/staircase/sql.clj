(ns staircase.sql
  (:import java.sql.SQLException)
  (:use [clojure.tools.logging :only (info error debug)]
        staircase.helpers)
  (:require
    [staircase.resources :refer (context)]
    [honeysql.core :as hsql]
    [honeysql.helpers :refer :all]
    [clojure.java.jdbc :as sql]))

;; Get the set of tables names.
(defn get-table-names [db]
  (sql/with-db-metadata [md db]
    (into #{} (sql/metadata-result
                  (.getTables md nil nil nil (into-array ["TABLE" "VIEW"]))
                  :row-fn :table_name
                  :result-set-fn doall))))

(defn get-column-names [db table]
  (sql/with-db-metadata [md db]
    (into #{} (sql/metadata-result
                (.getColumns md nil nil (name table) nil)
                :row-fn :column_name
                :result-set-fn doall))))

(defn log-sql-error [e]
  (error e)
  (when-let [ne (.getNextException e)]
    (log-sql-error ne)))

(defn count-where [conn tbl where]
  (let [query (hsql/format {:select [:%count.*] :from [tbl] :where where})]
    (debug "Count query" (pr-str query))
    (sql/query conn query :result-set-fn (comp :count first))))

(defn exists [conn tbl id]
  (when-let [uuid (string->uuid id)]
    (< 0 (count-where conn tbl [:= :id uuid]))))

(defn- owner-and-id-clause [uuid owner]
  [:and [:= :id uuid] [:= :owner owner]])

(defn exists-with-owner
  [conn tbl {id :id owner :user}]
  (when-let [uuid (string->uuid id)]
    (< 0 (count-where conn tbl (owner-and-id-clause uuid owner)))))

(defn- kw-keys [m]
  (reduce-kv #(assoc %1 (keyword %2) %3) {} m))

;; Filter to pass through entity values when updating.
;; It strips out things that cannot be changed and
;; makes sure all keys are keywords.
(defn- normalise-entity-values [values]
  (-> (kw-keys values) (dissoc :id :owner))) 

(defn- update-entity-where [conn tbl values constraint]
  (let  [to-update (normalise-entity-values values)
         update-cmd (-> (update tbl) (sset to-update) (where constraint) (hsql/format))]
    (sql/execute! conn update-cmd)
    (sql/query conn (hsql/format {:select [:*] :from [tbl] :where constraint :limit 1}) :result-set-fn first)))

(defn delete-entity
  [conn tbl id]
  (when-let [uuid (string->uuid id)]
    (sql/delete! conn
                tbl
                ["id=? and owner=?" uuid (:user context)])
    nil))

(defn update-entity [conn tbl id values]
  (if-let [uuid (string->uuid id)]
    (update-entity-where conn tbl values [:= :id uuid])))

(defn update-owned-entity
  [conn tbl {id :id owner :user} values]
  (if-let [uuid (string->uuid id)]
    (update-entity-where conn tbl values
                         (owner-and-id-clause uuid owner))))

(defn drop-all-tables ;; Seriously - DON'T USE THIS if you are not writing a test!
  "Drops all the tables and ALL THE DATA"
  [db]
  (let [tables (get-table-names db)
        drop-commands (map #(str "drop table " % " cascade;") tables)]
    (if-not (zero? (count tables))
      (do
        (apply (partial sql/db-do-commands db) drop-commands)
        (info "Dropped" (count tables) "tables."))
      (info "No tables to drop."))))

