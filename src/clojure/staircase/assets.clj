(ns staircase.assets
  (:import de.sandroboehme.lesscss.LessCompiler) ;; replace this with org.lesscss once changes are merged.
  (:require [dieter.settings :as settings]
            [ring.util.codec :as codec]
            [clojure.java.io :as io])
  (:use clojure.tools.logging
        [dieter.asset.coffeescript :only (compile-coffeescript)]))

(def less-c (LessCompiler.))

(defn less [f] (.compile less-c f))

(defn ext
  [f]
  (let [fname (.getName f)
        dotIdx (.lastIndexOf fname ".")]
    (keyword (.substring fname (+ 1 dotIdx)))))

(defmulti checksum ext)

(defmethod checksum :coffee
  [f]
  (let [md5     (java.security.MessageDigest/getInstance "MD5")
        content (-> (slurp f) (.getBytes "UTF-8"))
        digest  (.digest md5 content)]
    (BigInteger. 1 digest)))

(defmethod checksum :less
  [f]
  (let [dir (.getParentFile f)
        files (filter #(.isFile %) (file-seq dir))
        md5 (java.security.MessageDigest/getInstance "MD5")]
    (doseq [f' files]
      (.update md5 (-> (slurp f') (.getBytes "UTF-8"))))
    (BigInteger. 1 (.digest md5))))

(defn- get-kind [file-name]
  (cond (.endsWith file-name ".js") :script
        (.endsWith file-name ".coffee") :script
        (.endsWith file-name ".css") :style))

(defn asset-file-for [req options]
  (let [file-name (-> (:uri req)
                 codec/url-decode
                 (.replaceAll "\\.js" ".coffee") ;; needs refactoring
                 (.replaceAll "\\.css" ".less") ;; needs refactoring
                 (.replaceAll (options :js-dir) "")
                 (.replaceAll (options :css-dir) ""))
        path     (case (get-kind (:uri req))
                   :script (options :coffee)
                   :style (options :less)
                   "")
        file     (->> file-name (str (options :as-resource)) io/resource io/as-file)
        path-file(io/as-file (str path file-name))]
    (cond
      (and file (.exists file)) file
      (.exists path-file) path-file)))

(defn has-suffix
  [req suffix]
  (.endsWith (:uri req) suffix))

(defn is-asset-req [req]
  (and (= :get (:request-method req))
       (or (has-suffix req ".js")
           (has-suffix req ".coffee")
           (has-suffix req ".css"))))

(def asset-cache (atom {}))

(defn generate-response [asset-file req]
  (case (get-kind (:uri req))
    :script {:body (compile-coffeescript asset-file)
             :status 200
             :headers {"Content-Type" "text/javascript"}}
    :style {:body (less asset-file)
            :status 200
            :headers {"Content-Type" "text/css"}}
    {:status 500 :body "Unknown asset type" :content-type "text/plain"})) ;; Should never happen.

(defn serve-asset [req file options]
  (debug "Asset file found:" file)
  (if (:no-cache? options)
    (generate-response file req)
    (let [cksm (checksum file)
          cache-record (@asset-cache file)]
      (if (= (:hash cache-record) cksm)
        (do (debug "Serving asset from cache") (:resp cache-record))
        (do
          (debug "Generating new response for" file)
          (let [response (generate-response file req)
                cache-record {:hash cksm :resp response}]
            (swap! asset-cache assoc file cache-record)
            response))))))

;; Take options and return a function that will wrap a ring handler in an assets pipeline.
(defn pipeline [& {:as options}]
  (fn [app]
    (fn [req]
      (if-not (is-asset-req req)
        (app req)
        (settings/with-options options
          (if-let [asset-file (asset-file-for req options)]
            (serve-asset req asset-file options)
            (app req)))))))
