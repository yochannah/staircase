;; Copyright (c) 2014, 2015 Alex Kalderimis
;; All rights reserved.
;;
;; Redistribution and use in source and binary forms, with or without
;; modification, are permitted provided that the following conditions
;; are met:
;;
;; 1. Redistributions of source code must retain the above copyright
;;    notice, this list of conditions and the following disclaimer.
;; 2. Redistributions in binary form must reproduce the above copyright
;;    notice, this list of conditions and the following disclaimer in the
;;    documentation and/or other materials provided with the distribution.
;;
;; THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
;; IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
;; OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
;; IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
;; INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
;; NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
;; DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
;; THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
;; (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
;; THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

(ns staircase.main
  "The Main entry point for the staircase application.
  
    java -cp target/staircase-standalone.jar staircase.main

  Contains functions for staring and and stopping the web-application. Normally
  you would not need to call these functions directly; instead, call them
  from leingingen:

    lein run
  
  "
  (:gen-class)
  (:use [org.httpkit.server :only (run-server)]
        [environ.core :only (env)]
        [clojure.tools.logging :only (info)]
        [staircase.app :as app]))

(defn- to-int [s] (Integer/parseInt s 10))

(defonce ^:private servers (atom {}))

(defn stop-server
  "Stop the server associated with the given port"
  [port]
  (when-let [app (get @servers port)]
    (app :timeout 100) ;; Close port
    (swap! servers dissoc port))) ;; Delete ref.

(defn start-server
  "Start a server on the given port"
  [port]
  (let [port (or port 3000)]
    (stop-server port)
    (swap! servers
           assoc port (run-server (app/new-handler) {:port port}))
    (info "Started httpkit server on port" port)))

(defn -main
  "Start a web server.

   arguments: port - the port to listen on (optional).
  "
  [ & [userport] ]
  (let [port (to-int (or userport (get env :port "3000")))]
    (start-server port)))

