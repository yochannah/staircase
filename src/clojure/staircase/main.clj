(ns staircase.main
  (:gen-class)
  (:use [org.httpkit.server :only (run-server)]
        [environ.core :only (env)]
        [clojure.tools.logging :only (info)]
        [staircase.app :as app]))

(defn- to-int [s] (Integer/parseInt s 10))

(defonce servers (atom {}))

(defn stop-server
  [port]
  (when-let [app (get @servers port)]
    (app :timeout 100) ;; Close port
    (swap! servers dissoc port))) ;; Delete ref.

(defn start-server
  [port]
  (let [port (or port 3000)]
    (stop-server port)
    (swap! servers
           assoc port (run-server app/handler {:port port}))
    (info "Started staircase server on port" port)))

;; Start a web-server
;; arguments: port - the port to listen on (optional).
(defn -main [ & [userport] ]
  (let [port (to-int (or userport (get env :port "3000")))]
    (start-server port)))

