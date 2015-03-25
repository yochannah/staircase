(ns staircase.routing
  (:use staircase.protocols
        ring.util.response))

(def SUCCESS {:status 200})
(def ACCEPTED {:status 204})
(def NOT_FOUND {:status 404})

(defn get-resource [rs id]
  (if-let [ret (get-one rs id)]
    (response ret)
    NOT_FOUND))

;; Have to vectorise, since lazy seqs won't be jsonified.
(defn get-resources [rs] (response (vec (get-all rs))))

(defn create-new [rs doc]
  (let [id (create rs doc)]
    (get-resource rs id)))

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
