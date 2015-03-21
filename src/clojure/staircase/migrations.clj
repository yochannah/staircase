(ns staircase.migrations
  (:import [org.flywaydb.core Flyway]))

;; Thin wrapper around the Flyway java API.
(defn migrate [datasource]
  (let [flyway (doto (Flyway.)
                 (.setDataSource datasource)
                 (.setSqlMigrationPrefix ""))]
    (.migrate flyway)))
