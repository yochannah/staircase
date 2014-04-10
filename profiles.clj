{
 :dev {
      :dependencies [[ring-mock "0.1.5"]]
      :env { :db-subname "//localhost/staircase" }
 }
 :test {
        :env { :db-subname "//localhost/staircase-test" }
 }
 :travis {
        :env { :db-user "postgres" }
 }
}
