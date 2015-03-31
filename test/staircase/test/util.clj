(ns staircase.test.util
  (:require staircase.resources
            [cheshire.core :as json]
            [ring.middleware.session.memory :refer (memory-store)]
            [com.stuartsierra.component :as component])
  (:use 
        [staircase.helpers :only (keywordly-keyed)]
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

(defn find-by-id
  [id]
  (comp #{(str id)} str #(get % "id")))

(defrecord MockResource [things]
  ;; Immutable in-memory mock resource, holding a store of "things" keyed by their :id field.
  Resource

  (exists? [this id] (not (nil? (get-one this id))))
  (get-all [_] things)
  (get-one [_ id] (first (filter #(= (str (:id %1)) (str id)) things)))
  (update [this id doc] (merge (get-one this id) doc))
  (delete [this id] (when (exists? this id) id))
  (create [_ doc] (or (doc :id) (doc "id") (count things)))

  ;; the things may optionally have :children, which are used to mock subindexing.
  SubindexedResource

  (get-child [this id child-id]
    (when-let [thing (get-one this id)]
      (first (filter (comp #{child-id} :id) (:children thing)))))

  (delete-child [this id child-id] ;; Return the id we 'deleted', if we have it.
    (:id (get-child this id child-id)))

  (add-child [this id child]
    (when-let [thing (get-one this id)]
      (count (:children thing)))))

(defrecord MockStatefulResource [things serial]
  ;; Implementation of resource that stores its state in an atom.
  Resource

  (exists? [this id] (not (nil? (get-one this id))))

  (get-all [_]
    (map keywordly-keyed
         (filter #(= (% "owner") (:user staircase.resources/context))
                 @things)))

  (get-one [this id]
    (first (filter #(= (str id)
                       (str (:id %)))
                   (get-all this))))

  (update [this id doc]
    (swap! things (partial map #(if (= (str id) (str (% "id"))) (merge % doc) %)))
    (get-one this id))
  (delete [this id]
    (swap! things (partial filter #(not (= (str id) (str (% "id")))))))
  (create [_ doc]
    (let [id (swap! serial inc)]
      (swap! things conj (assoc doc "id" id "owner" (:user staircase.resources/context)))
      id))
  
  SubindexedResource

  ;; Finds the child by id of an existing item - or nil if it does not exist.
  (get-child [this id child]
    (when-let [thing (get-one this id)]
      (first (filter (find-by-id child) (:children thing)))))

  ;; Adds a new child to the existing thing, if it exists, returning the new id.
  (add-child [this id child]
    (when-let [thing (get-one this id)]
      (let [next-id (swap! serial inc)
            child (assoc child "id" next-id)
            finder (find-by-id id)
            kids (conj (or (:children thing) []) child)]
        (letfn [(update-kids [x] (if (finder x) (assoc x :children kids) x))]
          (swap! things (partial map update-kids)))
        next-id)))

  ;; Removes the child with the given id from the thing with that id, if it exists.
  (delete-child [this id child-id]
    (when (exists? this id)
      (letfn [(remove-child [kids] (filter (comp (complement #{child-id}) :id) kids))
              (update-kids [x] (if (= id (x "id")) (update-in x [:children] remove-child) x))]
        (swap! things map (partial map update-kids)))
      child-id))
  )

(defn atomic-resource [things]
  (map->MockStatefulResource {:serial (atom (count things)) :things (atom things)}))

;; Construct a ring handler very simply.
(defn get-router [resources]
  (-> (new-router)
      (assoc :asset-pipeline asset-pipeline
             :config (atom {})
             :secrets {:key-phrase "some very difficult key-phrase"}
             :session-store (memory-store)
             :resources resources)
      (component/start)
      :handler))
