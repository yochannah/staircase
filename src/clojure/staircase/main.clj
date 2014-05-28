(ns staircase.main
  (:gen-class)
  (:use [org.httpkit.server :only (run-server)]
        [environ.core :only (env)]
        [clojure.tools.logging :only (info)]
        [staircase.app :as app]))

(defn- to-int [s] (Integer/parseInt s))

(defonce server (atom nil))

(defn stop-server
  []
  (when-not (nil? @server)
    (@server :timeout 100)
    (reset! server nil)))

(defn -main [ & args ]
  (let [port (to-int (get env :port "3000"))]
    (reset! server (run-server app/handler {:port port}))
    (info "Started staircase server on port" port)))

