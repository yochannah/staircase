(ns staircase.helpers)

(defn new-id [] (java.util.UUID/randomUUID))

(defn string->uuid [id] 
  (if (instance? java.util.UUID id)
    id
    (try
      (java.util.UUID/fromString id)
      (catch IllegalArgumentException e nil))))

(defn now [] (java.util.Date.))
