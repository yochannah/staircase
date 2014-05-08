(ns staircase.helpers)

(defn new-id [] (java.util.UUID/randomUUID))

(defn string->uuid [id] 
  (if (instance? java.util.UUID id)
    id
    (try
      (java.util.UUID/fromString id)
      (catch NullPointerException e nil)
      (catch IllegalArgumentException e nil))))

(defn now [] (java.util.Date.))

(defn sql-now [] (java.sql.Timestamp. (.getTime (now))))
