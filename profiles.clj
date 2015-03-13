{
 :dev {
      :dependencies [[peridot "0.2.2"]
                     [org.marianoguerra/clj-rhino "0.2.1"]
                     [ring-mock "0.1.5"]]
      :env {
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
            :web-default-service "flymine"
            :web-audience "http://localhost:3000"

            :verifier "https://verifier.login.persona.org/verify"
            :db-subname "//localhost/staircase" 
            :db-user "postgres"
            :db-password "password"}
 }
 :test {
        :bower-dependencies [[angular-scenario "~1.2.9"]
                             [angular-mocks "~1.2.9"]
                             [should "~3.3.1"]]
        :env { :db-subname "//localhost/staircase-test" }
        :resource-paths ["test/resources"]
 }
 :travis {
        :env { :db-user "postgres" }
 }
}
