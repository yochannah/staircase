(ns leiningen.get-in-project)

(defn get-in-project [project & coords]
  "Read values from the project map"
  (println (get-in project (map read-string coords))))
