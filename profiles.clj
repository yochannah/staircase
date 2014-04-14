{
 :dev {
      :dependencies [[ring-mock "0.1.5"]]
      :env {
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
