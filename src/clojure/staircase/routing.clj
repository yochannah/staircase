(ns staircase.routing
  (:require [compojure.response :refer (Renderable)])
  (:use staircase.protocols
        ring.util.response))

(def default-headers {})

(def SUCCESS {:status 200 :headers default-headers})
(def ACCEPTED {:status 204 :headers default-headers})
(def NOT_FOUND {:status 404 :headers default-headers})
(def CLIENT_ERROR {:status 400 :headers default-headers})

(defn serialise-link
  [[uri rel]]
  (str "<" uri ">; rel=\"" rel "\""))

(defn get-resource
  "Fetch a single resource, or return NOT FOUND. Add a HATEOS link if provided"
  [rs id & {:keys [linker] ;; Optional linker function, which provides one or more links in a seq.
            :or {linker (constantly [])}}]
  (if-let [ret (get-one rs id)]
    (update-in (response ret)
              [:headers "Link"]
              concat (map serialise-link (linker ret)))
    NOT_FOUND))

;; Have to vectorise, since lazy seqs won't be jsonified.
(defn get-resources [rs] (response (vec (get-all rs))))

(defn create-new [rs doc]
  (try
    (let [id (create rs doc)]
      (get-resource rs id))
    (catch clojure.lang.ExceptionInfo e
      (if (= :constraint-violation (-> e ex-data :type))
        (assoc CLIENT_ERROR :body (ex-data e))
        (throw e)))))

(defn update-resource [rs id doc]
  (if (exists? rs id)
    (response (update rs id doc))
    NOT_FOUND))

(defn delete-resource [rs id]
  (if (exists? rs id)
    (do 
      (delete rs id)
      ACCEPTED)
    NOT_FOUND))

(extend-protocol Renderable
  clojure.lang.PersistentVector ;; Enable compojure routes to return vectors.
  (render [body req] (response body)))
