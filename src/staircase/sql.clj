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

(defn create-tables [conn table-specs]
  (let [tables (get-table-names conn)]
    (sql/with-db-connection [c conn]
      (doseq [table (keys table-specs)]
        (debug "Currently existing tables:" tables)
        (when (not (contains? tables (name table)))
          (debug "Creating table:" table)
          (try
            (sql/db-do-commands c (apply sql/create-table-ddl table (table table-specs)))
            (catch SQLException e
              (do (log-sql-error e) (throw (Exception. (str "Could not create table: " table) e))))))))))

(defn update-entity [conn tbl id values]
  (if-let [uuid      (string->uuid id)]
    (let  [to-update (dissoc values "id" :id)]
      (sql/update! conn tbl to-update ["id=?" uuid])
      (sql/query conn (hsql/format {:select [:*] :from [tbl] :where [:= :id uuid] :limit 1}) :result-set-fn first))))

