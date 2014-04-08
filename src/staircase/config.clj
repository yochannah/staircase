(ns staircase.config
  (:use [clojure.java.io :as io])
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

