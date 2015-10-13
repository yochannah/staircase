{
 :uberjar {
    :aot [staircase.main]}
 :dev {
      :hooks [leiningen.compile-assets/hooks]
      :source-paths ["dev/clojure"]
      :dependencies [[peridot "0.2.2"]
                     [org.marianoguerra/clj-rhino "0.2.1"]
                     [org.mozilla/rhino "1.7R4"] ;; We depend on rhino 1.7r4
                     [org.lesscss/lesscss "1.7.0.1.1"] ;; Less 1.7.0
                     [org.clojars.involans/dieter "0.5.0-SNAPSHOT"
                        :exclusions [com.google.javascript/closure-compiler]]
                     [ring-mock "0.1.5"]]
      :env {
            :dev true ;; So we know if we are in the dev environment.
            :web-search-placeholder "zen, diabetes, apoptosis"
            :web-project-title "FlyMine"
            :web-contact-email "alex@intermine.org"
            :web-contacts [
                           ["fa-twitter" "https://twitter.com/intermineorg" "@intermineorg"]
                           ["fa-stack-overflow"
                            "http://stackoverflow.com/search?q=intermine"
                            "Stack Overflow"]
                           ["fa-envelope" "mailto://dev@intermine.org" "Mailing list"]
                           ]
            :web-max-age 300
            :web-default-service "flymine-beta"
            :web-audience "http://localhost:3000"

            :verifier "https://verifier.login.persona.org/verify"
            :db-subname "//localhost/staircase"
          :db-user "josh"}
 }
 :test {
        :bower-dependencies [[angular-scenario "~1.2.9"]
                             [angular-mocks "~1.2.9"]
                             [should "~3.3.1"]]
        :env {
              :live-asset-reload true
              :db-subname "//localhost/staircase-test" }
        :resource-paths ["test/resources"]
 }
 :travis {
        :env {
              :live-asset-reload true
              :db-user "postgres" }
 }
}
