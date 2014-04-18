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

(defn secrets [] (try (from-edn "secrets.edn") (catch Exception e {}))) ;; Empty secrets if not configured.

(def configuration (delay (from-edn "config.edn")))

(defn config [k]
  (k @configuration))

(defn- options [opts prefix]
  (->> opts
       keys 
       (map name) ;; From keyword -> str
       (filter #(.startsWith % prefix))
       (reduce #(assoc %1 (keyword (replace-first %2 prefix "")) (opts (keyword %2))) {})))

(defn db-options [opts]
  (options opts "db-"))

(defn app-options [opts]
  (options opts "web-"))

