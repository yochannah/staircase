(defproject staircase "0.1.0-SNAPSHOT"
  :description "The application holding data-flow steps."
  :url "http://steps.intermine.org"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.logging "0.2.6"] ;; Logging
                 [org.clojure/tools.reader "0.8.4"] ;; Read edn
                 [compojure "1.1.6"] ;; Request handlers
                 [ring/ring-json "0.1.2"] ;; JSON marshalling
                 [ring-mock "0.1.5"] ;; For testing request handlers.
                 [c3p0/c3p0 "0.9.1.2"] ;; DB pooling
                 [com.stuartsierra/component "0.2.1"] ;; Dependency management
                 [honeysql "0.4.3"] ;; SQL sugar
                 [org.clojure/java.jdbc "0.2.3"] ;; DB interface
                 [postgresql/postgresql "8.4-702.jdbc4"] ;; DB Driver
                 [cheshire "4.0.3"]] ;; JSON serialisation
  :plugins [[lein-ring "0.8.10"]]
  :ring {:handler staircase.app/handler}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring-mock "0.1.5"]]}})
