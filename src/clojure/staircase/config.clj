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

(defn- parse-url
  [url]
  (let [parsed (java.net.URI. url)
        [user password] (.split (.getUserInfo parsed) ":")]
    (str "jdbc:postgresql://" (.getHost parsed) ":" (.getPort parsed) (.getPath parsed) "?user=" user "&password=" password)))

(defn db-options [opts]
  (if-let [url (:database-url opts)]
    {:connection-uri (parse-url url)}
    (options opts "db-")))

(defn app-options [opts]
  (options opts "web-"))

