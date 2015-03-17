(ns staircase.test.util
  (:require staircase.resources
            [cheshire.core :as json]
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

(defrecord MockResource [things]
  ;; Immutable in-memory mock resource, holding a store of "things" keyed by their :id field.
  Resource

  (exists? [this id] (not (nil? (get-one this id))))
  (get-all [_] things)
  (get-one [_ id] (first (filter #(= (str (:id %1)) (str id)) things)))
  (update [this id doc] (merge (get-one this id) doc))
  (delete [_ id])
  (create [_ doc] (or (doc :id) (doc "id") (count things))))

(defrecord MockStatefulResource [things serial]
  ;; Implementation of resource that stores its state in an atom.
  Resource

  (exists? [this id] (not (nil? (get-one this id))))
  (get-all [_]
    (filter #(= (% "owner") (:user staircase.resources/context)) @things))
  (get-one [this id]
    (first (filter #(= (str (%1 "id")) (str id)) (get-all this))))
  (update [this id doc]
    (swap! things map #(if (= id (% "id")) (merge % doc) %))
    (get-one this id))
  (delete [_ id]
    (swap! things filter #(not (= id (% "id")))))
  (create [_ doc]
    (let [id (swap! serial inc)]
      (swap! things conj (assoc doc "id" id "owner" (:user staircase.resources/context)))
      id)))

(defn atomic-resource [things]
  (map->MockStatefulResource {:serial (atom (count things)) :things (atom things)}))

(defn get-router [histories steps]
  (-> (new-router)
      (assoc :asset-pipeline asset-pipeline
             :config (atom {})
             :secrets {:key-phrase "some very difficult key-phrase"}
             :session-store (memory-store)
             :histories histories
             :steps steps)
      (component/start)
      :handler))
