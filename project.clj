(defproject staircase "0.1.0-SNAPSHOT"
  :description "The application holding data-flow steps."
  :url "http://steps.herokuapp.org"
  :main staircase.main
  :aot [staircase.main]
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.logging "0.2.6"] ;; Logging
                 [org.clojure/tools.reader "0.8.4"] ;; Read edn
                 [org.clojure/algo.monads "0.1.5"] ;; Monadic interfaces.
                 [org.clojure/java.jdbc "0.3.3"] ;; DB interface
                 [clj-http "0.9.1"] ;; Perform http requests.
                 [http-kit "2.1.16"]
                 [clj-jwt "0.0.12"] ;; Generate signed json web-tokens.
                 [persona-kit "0.1.1-SNAPSHOT"] ;; Authentication - Persona.
                 [com.cemerick/friend "0.2.0"] ;; Authentication.
                 [com.cemerick/drawbridge "0.0.6"] ;; remote debugging
                 [ring-basic-authentication "1.0.5"] ;; basic auth for repl access
                 [clj-time "0.6.0"] ;; deal with time.
                 [javax.servlet/servlet-api "2.5"] ;; Needed for middleware.
                 [honeysql "0.4.3"] ;; SQL sugar
                 [postgresql/postgresql "8.4-702.jdbc4"] ;; DB Driver
                 [compojure "1.1.6"] ;; Request handlers
                 [ring-middleware-format "0.3.2"] ;; JSON marshalling
                 [ring "1.2.2"] ;; sessions.
                 [ring/ring-json "0.3.1"]
                 [ring/ring-anti-forgery "0.3.1"] ;; CSRF protection.
                 [ring-cors "0.1.1"]
                 [c3p0/c3p0 "0.9.1.2"] ;; DB pooling
                 [com.stuartsierra/component "0.2.1"] ;; Dependency management
                 [environ "0.4.0"] ;; Settings management.
                 [cheshire "4.0.3"];; JSON serialisation
                 [clj-jgit "0.6.5-d"] ;; Git interface.
                 [org.mozilla/rhino "1.7R4"] ;; We depend on rhino 1.7r4
                 [org.clojars.involans/dieter "0.5.0-SNAPSHOT"
                    :exclusions [com.google.javascript/closure-compiler]]
                 [org.lesscss/lesscss "1.7.0.1.1"] ;; Less 1.7.0 
                 [jdbc-ring-session "0.2"]
                 [hiccup "1.0.5"] ;; Templating
                 ;; Deal with load issues.
                 ;; see: https://github.com/LightTable/LightTable/issues/794
                 [org.clojure/core.cache "0.6.3"] 
                 [org.clojure/core.memoize "0.5.6" :exclusions [org.clojure/core.cache]]
                 [log4j/log4j "1.2.17"]] ;; Logging
  :min-lein-version "2.0.0"
  :plugins [[com.jakemccrary/lein-test-refresh "0.4.0"]
            [lein-bower "0.4.0"]
            [lein-environ "0.4.0"]
            [lein-pprint "1.1.1"]
            [lein-ring "0.8.10"]]
  :lesscss-paths ["src/less"]
  :lesscss-output-path "resources/public/css"
  :ring {:handler staircase.app/handler}
  :prep-tasks ["javac" "compile" "clean-tools" "load-tools"]
  :source-paths ["src/clojure"]
  :resource-paths ["resources" "external"]
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
      :web-search-placeholder "enter a search term"
      :web-gh-repository "https://github.com/alexkalderimis/staircase"
      :db-classname "org.postgresql.Driver"
      :db-subprotocol "postgresql"
      :web-project-title "InterMine"
      :web-max-session-age ~(* 60 60 24)
      :web-max-age ~(* 30 24 60 60)
      :web-contacts [["fa-github" "https://github.com/alexkalderimis/staircase" "GitHub"]]
      :web-services {
        "flymine" "http://www.flymine.org/query/service"
        "zfin" "http://www.zebrafishmine.org/service"
        "yeastmine" "http://yeastmine.yeastgenome.org/yeastmine/service"
        "mousemine" "http://www.mousemine.org/mousemine/service"}
      ;; The section below should be replaced by pulling these values from branding.
      ;; and ultimately by a template based solution.
      :web-service-meta {
                    "flymine"   {:color "palette-5tone1" :covers ["D. melanogaster"]}
                    "zfin"      {:color "palette-5tone2" :covers ["D. rerio"]}
                    "mousemine" {:color "palette-5tone3" :covers ["M. musculus"]}
                    "yeastmine" {:color "palette-5tone4" :covers ["S. cerevisiae"]}}
      :client-ga-token nil ;; Supply a token to use analytics
      :client-whitelist [
        "http://*.labs.intermine.org/**"
        "http://tools.intermine.org/**"
        "http://alexkalderimis.github.io/**"
        "http://intermine.github.io/**"]
      :client-step-config {
        :show-list {
                    :activeTabs [:enrich]}
        :show-table {
                     :TableCell {
                                 :IndicateOffHostLinks false
                                 :PreviewTrigger :click}
                     :ShowHistory false
                     :Style {:icons :fontawesome}}}
      :web-tools [ ;; Needs to be listed so we know what order these should be shown in.
                   :histories
                   :templates
                   [:choose-list {:service "flymine"}]
                   [:choose-list {:service "mousemine"}]
                   [:new-query {:service "flymine"}]
                   [:new-query {:service "yeastmine"}]
                   [:upload-list {:service "flymine"}]
                   [:region-search {:service "flymine"}]
                   [:region-search {:service "mousemine"}]
                   :show-table ;; TODO: make the tools below autoconfigure...
                   :show-list  ;;  - these are not front page, so their order is not important.
                   :show-enrichment
                   :resolve-ids
                   :combine-lists
                   :convert-list
                   :list-templates
                   :export
                   :id-handler
                   :keyword-search ;; From resources/config - really must auto-configure this list...
                   ]
  }
)
