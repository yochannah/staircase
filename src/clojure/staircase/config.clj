(ns staircase.config
  (:use [clojure.java.io :as io]
        [clojure.string :only (replace-first)])
  (:require [clojure.tools.reader.edn :as edn]))

(defn- from-edn [fname]
  (if-let [is (io/resource fname)]
    (with-open [rdr (-> is
                        io/reader
                        java.io.PushbackReader.)]
      (edn/read rdr))
    (throw (IllegalArgumentException. (str fname " not found")))))

(defn- options [opts prefix]
  (->> opts
       keys 
       (map name) ;; From keyword -> str
       (filter #(.startsWith % prefix))
       (reduce #(assoc %1 (keyword (replace-first %2 prefix "")) (opts (keyword %2))) {})))

;; Empty secrets if not configured.
(defn secrets [opts]
  (merge (try (from-edn "secrets.edn") (catch Exception e {}))
         (options opts "secret-")))

(def configuration (delay (from-edn "config.edn")))

(defn config [k]
  (k @configuration))

(defn db-options [opts]
  (if-let [uri (:database-url opts)]
    {:connection-uri uri}
    (options opts "db-")))

(defn app-options [opts]
  (options opts "web-"))

