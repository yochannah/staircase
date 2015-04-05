;; Copyright (c) 2014, 2015 Alex Kalderimis
;; All rights reserved.
;;
;; Redistribution and use in source and binary forms, with or without
;; modification, are permitted provided that the following conditions
;; are met:
;;
;; 1. Redistributions of source code must retain the above copyright
;;    notice, this list of conditions and the following disclaimer.
;; 2. Redistributions in binary form must reproduce the above copyright
;;    notice, this list of conditions and the following disclaimer in the
;;    documentation and/or other materials provided with the distribution.
;;
;; THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
;; IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
;; OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
;; IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
;; INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
;; NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
;; DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
;; THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
;; (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
;; THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

(ns staircase.sql
  "Functions for interacting directly with the database.

  This is the most low level set of functions for accessing the datastore."
  (:import java.sql.SQLException)
  (:use [clojure.tools.logging :only (info error debug)]
        staircase.helpers)
  (:require
    [staircase.resources :refer (context)]
    [honeysql.core :as hsql]
    [honeysql.helpers :refer :all]
    [clojure.java.jdbc :as sql]))

;; Get the set of tables names.
(defn get-table-names
  "Get all the names of the tables in the database"
  [db]
  (sql/with-db-metadata [md db]
    (into #{} (sql/metadata-result
                  (.getTables md nil nil nil (into-array ["TABLE" "VIEW"]))
                  :row-fn :table_name
                  :result-set-fn doall))))

(defn get-column-names
  "Get the names of the columns on the given table"
  [db table]
  (sql/with-db-metadata [md db]
    (into #{} (sql/metadata-result
                (.getColumns md nil nil (name table) nil)
                :row-fn :column_name
                :result-set-fn doall))))

(defn log-sql-error
  "Log an SQL error"
  [e]
  (error e)
  (when-let [ne (.getNextException e)]
    (log-sql-error ne)))

(defn count-where
  "Return the number of rows returned with applying the given constraint to the given table"
  [conn tbl where]
  (let [query (hsql/format {:select [:%count.*] :from [tbl] :where where})]
    (debug "Count query" (pr-str query))
    (sql/query conn query :result-set-fn (comp :count first))))

(defn exists
  "Return true if there is a row in the given table with the given id"
  [conn tbl id]
  (when-let [uuid (string->uuid id)]
    (< 0 (count-where conn tbl [:= :id uuid]))))

(defn- owner-and-id-clause [uuid owner]
  [:and [:= :id uuid] [:= :owner owner]])

(defn exists-with-owner
  "Return true if there is a row in the given table with the given id belonging to the given user"
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
  "Delete the row in the table with the given id if it belongs to the current user"
  [conn tbl id]
  (when-let [uuid (string->uuid id)]
    (sql/delete! conn
                tbl
                ["id=? and owner=?" uuid (:user context)])
    nil))

(defn update-entity
  "Update the row in the database table with the given id"
  [conn tbl id values]
  (if-let [uuid (string->uuid id)]
    (update-entity-where conn tbl values [:= :id uuid])))

(defn update-owned-entity
  "Update the row in the table with the given id that belongs to the given user"
  [conn tbl {id :id owner :user} values]
  (if-let [uuid (string->uuid id)]
    (update-entity-where conn tbl values
                         (owner-and-id-clause uuid owner))))

(defn drop-all-tables
  "Drops all the tables and ALL THE DATA.

  Seriously - DON'T USE THIS if you are not writing a test!
  The ONLY SANE REASON for using this is writing test fixtures.
  "
  [db]
  (let [tables (get-table-names db)
        drop-commands (map #(str "drop table " % " cascade;") tables)]
    (if-not (zero? (count tables))
      (do
        (apply (partial sql/db-do-commands db) drop-commands)
        (info "Dropped" (count tables) "tables."))
      (info "No tables to drop."))))

