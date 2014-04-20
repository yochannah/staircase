(ns staircase.test.util
  (:require [cheshire.core :as json]
            [ring.middleware.session.memory :refer (memory-store)]
            [com.stuartsierra.component :as component])
  (:use 
        [staircase.handler :only (new-router)]
        ring.mock.request  
        staircase.protocols))

(defn asset-pipeline [f] f)

(defn json-request [meth path & [payload]]
  (let [req (-> (request meth path)
                (header "Accept" "application/json"))]
    (if payload
      (-> req (body (json/generate-string payload)) (content-type "application/json;charset=utf-8"))
      req)))

;; In-memory mock resource, holding a store of "things" keyed by their :id field.
(defrecord MockResource [things]
  Resource

  (exists? [this id] (not (nil? (get-one this id))))
  (get-all [_] things)
  (get-one [_ id] (first (filter #(= (str (:id %1)) (str id)) things)))
  (update [this id doc] (merge (get-one this id) doc))
  (delete [_ id])
  (create [_ doc] (doc "id")))

(defn get-router [histories steps]
  (-> (new-router)
      (assoc :asset-pipeline asset-pipeline
             :config {}
             :secrets {}
             :session-store (memory-store)
             :histories histories
             :steps steps)
      (component/start)
      :handler))
