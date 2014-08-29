(ns staircase.main
  (:gen-class)
  (:use [org.httpkit.server :only (run-server)]
        [environ.core :only (env)]
        [clojure.tools.logging :only (info)]
        [staircase.app :as app]))

(defn- to-int [s] (Integer/parseInt s 10))

(defonce server (atom {}))

(defn stop-server
  [port]
  (when-let [app (get @server port)]
    (app :timeout 100) ;; Close port
    (swap! server dissoc port))) ;; Delete ref.

(defn start-server
  [port]
  (let [port (or port 3000)]
    (stop-server port)
    (swap! server assoc port (run-server app/handler {:port port}))
    (info "Started staircase server on port" port)))

(defn -main [ & [userport] ]
  (let [port (to-int (or userport (get env :port "3000")))]
    (start-server port)))

