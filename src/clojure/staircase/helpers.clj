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

;; Take a mapping with keys that are keywords or strings and
;; return a mapping with only string keys.
(defn stringly-keyed [mapping]
  (reduce (fn [m [k v]] (-> m (dissoc k) (assoc (name k) v)))
          mapping
          (filter (comp keyword? first) mapping)))

;; Take a mapping with keys that are keywords or strings and
;; return a mapping with only keyword keys.
(defn keywordly-keyed [mapping]
  (reduce (fn [m [k v]] (-> m (dissoc k) (assoc (keyword k) v)))
          mapping
          (filter (comp (complement keyword?) first) mapping)))

