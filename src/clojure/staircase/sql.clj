(ns staircase.sql
  (:import java.sql.SQLException)
  (:use [clojure.tools.logging :only (info error debug)]
        staircase.helpers)
  (:require
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

(defn exists-with-owner [conn tbl id owner]
  (when-let [uuid (string->uuid id)]
    (< 0 (count-where conn tbl [:and [:= :id uuid] [:= :owner owner]]))))

;; super naive stringification - relies on no funny stuff.
;; ONLY SUITABLE FOR CONTROLLED DATA such as the schema.
(defn as-sql [xs] 
  (clojure.string/join " " (map #(if (keyword? %) (name %) %) xs)))

(defn check-columns [conn table col-defs]
  (let [columns (get-column-names conn table)]
    (doseq [coldef col-defs]
      (let [colname (name (coldef 0))]
        ;; Only need to act if it is a real column and it is missing.
        (when (and (not (columns colname)) (not (= "UNIQUE" colname)))
          (if (some #(= % "NOT NULL") coldef) ;; Cannot handle this.
            (throw (Exception. (str "Cannot add column " colname " to " table " because it is NOT NULL")))
            (do
              (info (str "Adding column " colname " to " table))
              (sql/execute! conn [(str "ALTER TABLE " (name table) " ADD " (as-sql coldef))]))))))))

(defn create-tables [conn table-specs]
  (let [tables (get-table-names conn)]
    (sql/with-db-connection [c conn]
      (doseq [table (keys table-specs)]
        (debug "Currently existing tables:" tables)
        (if (not (contains? tables (name table)))
          (do
            (info "Creating table:" table)
            (try
              (sql/db-do-commands c (apply sql/create-table-ddl table (table table-specs)))
              (catch SQLException e
                (do (log-sql-error e) (throw (Exception. (str "Could not create table: " table) e))))))
          (do
            (debug table " exists - checking columns")
            (check-columns conn table (table table-specs)))
          )))))

(defn- kw-keys [m]
  (reduce-kv #(assoc %1 (keyword %2) %3) {} m))

(defn- normalise-entity-values [values]
  (-> (kw-keys values) (dissoc :id :owner))) ;; These things may not be changed.

(defn- update-entity-where [conn tbl values constraint]
  (let  [to-update (normalise-entity-values values)
         update-cmd (-> (update tbl) (sset to-update) (where constraint) (hsql/format))]
    (sql/execute! conn update-cmd)
    (sql/query conn (hsql/format {:select [:*] :from [tbl] :where constraint :limit 1}) :result-set-fn first)))

(defn update-entity [conn tbl id values]
  (if-let [uuid (string->uuid id)]
    (update-entity-where conn tbl values [:= :id uuid])))

(defn update-owned-entity [conn tbl id owner values]
  (if-let [uuid (string->uuid id)]
    (update-entity-where conn tbl values [:and [:= :id uuid] [:= :owner owner]])))

