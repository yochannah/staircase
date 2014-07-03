{
 :dev {
      :dependencies [[peridot "0.2.2"]
                     [org.marianoguerra/clj-rhino "0.2.1"]
                     [ring-mock "0.1.5"]]
      :env {
            :web-project-title "FlyMine"
            :web-max-age 300
            :web-default-service "flymine"
            :web-audience "http://localhost:3000"
            :verifier "https://verifier.login.persona.org/verify"
            :db-subname "//localhost/staircase" }
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
