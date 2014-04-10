(ns staircase.config
  (:use [clojure.java.io :as io]
        [clojure.string :only (replace-first)])
  (:require [clojure.tools.reader.edn :as edn]))

(defn from-edn [fname]
  (if-let [is (io/resource fname)]
    (with-open [rdr (-> is
                        io/reader
                        java.io.PushbackReader.)]
      (edn/read rdr))
    (throw (IllegalArgumentException. (str fname " not found")))))

(defn config [env]
  (let [conf (from-edn "config.edn")
        env-conf (conf env)]
    (merge-with merge (:default conf) env-conf)))

(defn- options [opts prefix]
  (->> opts
       keys 
       (map name) ;; From keyword -> str
       (filter #(.startsWith % prefix))
       (reduce #(assoc %1 (keyword (replace-first %2 prefix "")) (opts (keyword %2))) {})))

(defn db-options [opts]
  (options opts "db-"))

