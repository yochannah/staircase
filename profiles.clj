{
 :dev {
      :dependencies [[peridot "0.2.2"]
                     [ring-mock "0.1.5"]]
      :env {
            :web-audience "http://localhost:3000"
            :verifier "https://verifier.login.persona.org/verify"
            :db-subname "//localhost/staircase" }
 }
 :test {
        :env { :db-subname "//localhost/staircase-test" }
 }
 :travis {
        :env { :db-user "postgres" }
 }
}
