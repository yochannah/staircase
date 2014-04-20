(defproject staircase "0.1.0-SNAPSHOT"
  :description "The application holding data-flow steps."
  :url "http://steps.intermine.org"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.logging "0.2.6"] ;; Logging
                 [org.clojure/tools.reader "0.8.4"] ;; Read edn
                 [org.clojure/algo.monads "0.1.5"] ;; Monadic interfaces.
                 [org.clojure/java.jdbc "0.3.3"] ;; DB interface
                 [clj-http "0.9.1"] ;; Perform http requests.
                 [clj-jwt "0.0.4"] ;; Generate signed json web-tokens.
                 [persona-kit "0.1.1-SNAPSHOT"] ;; Authentication - Persona.
                 [com.cemerick/friend "0.2.0"] ;; Authentication.
                 [clj-time "0.6.0"] ;; deal with time.
                 [javax.servlet/servlet-api "2.5"] ;; Needed for middleware.
                 [honeysql "0.4.3"] ;; SQL sugar
                 [postgresql/postgresql "8.4-702.jdbc4"] ;; DB Driver
                 [compojure "1.1.6"] ;; Request handlers
                 [ring-middleware-format "0.3.2"] ;; JSON marshalling
                 [ring "1.2.2"] ;; sessions.
                 [ring/ring-json "0.3.1"]
                 [ring/ring-anti-forgery "0.3.1"] ;; CSRF protection.
                 [c3p0/c3p0 "0.9.1.2"] ;; DB pooling
                 [com.stuartsierra/component "0.2.1"] ;; Dependency management
                 [environ "0.4.0"] ;; Settings management.
                 [cheshire "4.0.3"];; JSON serialisation
                 [clj-jgit "0.6.5-d"] ;; Git interface.
                 [dieter "0.4.1"] ;; Assets pipe-lining
                 [de.sandroboehme.lesscss/lesscss "1.6.1.1.0"] ;; Less 1.6.1
                 [hiccup "1.0.5"] ;; Templating
                 ;; Deal with load issues.
                 ;; see: https://github.com/LightTable/LightTable/issues/794
                 [org.clojure/core.cache "0.6.3"] 
                 [org.clojure/core.memoize "0.5.6" :exclusions [org.clojure/core.cache]]
                 [log4j/log4j "1.2.17"]] ;; Logging
  :plugins [[com.jakemccrary/lein-test-refresh "0.4.0"]
            [lein-environ "0.4.0"]
            [lein-pprint "1.1.1"]
            [lein-ring "0.8.10"]]
  :lesscss-paths ["src/less"]
  :lesscss-output-path "resources/public/css"
  :ring {:handler staircase.app/handler}
  :source-paths ["src/clojure"]
  :aliases {
            "js-deps" ["run" "-m" "staircase.tasks/js-deps"]
            "clean-js" ["run" "-m" "staircase.tasks/clean-js"]
            "load-tools" ["run" "-m" "staircase.tasks/load-tools"]
            "clean-tools" ["run" "-m" "staircase.tasks/clean-tools"]}
  :test-selectors {
                   :all (constantly true)
                   :database :database
                   :default (complement :acceptance)}
  :env {
        :web-max-session-age ~(* 60 60 24)
        :db-classname "org.postgresql.Driver"
        :db-subprotocol "postgresql"
  }
)
