{
 :dev {
      :dependencies [[peridot "0.2.2"]
                     [org.marianoguerra/clj-rhino "0.2.1"]
                     [ring-mock "0.1.5"]]
      :env {
            :web-services {"flymine" "http://www.flymine.org/query/service"}
            :web-default-service "flymine"
            :web-tools [
                        :templates
                        :choose-list
                        :new-query
                        :region-search
                        :upload-list
                        :histories
                        :hello-world
                        ]
            :web-audience "http://localhost:3000"
            :verifier "https://verifier.login.persona.org/verify"
            :db-subname "//localhost/staircase" }
 }
 :test {
        :env { :db-subname "//localhost/staircase-test" }
        :resource-paths ["test/resources"]
 }
 :travis {
        :env { :db-user "postgres" }
 }
}
