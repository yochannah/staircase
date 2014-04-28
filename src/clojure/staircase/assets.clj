(ns staircase.assets
  (:import de.sandroboehme.lesscss.LessCompiler) ;; replace this with org.lesscss once changes are merged.
  (:require [dieter.settings :as settings]
            [clojure.string  :as string]
            [ring.util.codec :as codec]
            [clojure.java.io :as io])
  (:use clojure.tools.logging
        [dieter.asset.livescript   :only (compile-livescript)]
        [dieter.asset.coffeescript :only (compile-coffeescript)]))

(def less-c (LessCompiler.))

(defn less [f] (.compile less-c f))

(defn ext
  [f]
  (when f
    (let [fname (.getName f)
          dotIdx (.lastIndexOf fname ".")]
      (keyword (.substring fname (+ 1 dotIdx))))))

(defn- file-md5
  [f]
  (let [md5     (java.security.MessageDigest/getInstance "MD5")
        content (-> (slurp f) (.getBytes "UTF-8"))
        digest  (.digest md5 content)]
    (BigInteger. 1 digest)))

(defn- dir-md5
  [f]
  (let [dir (.getParentFile f)
        files (filter #(.isFile %) (file-seq dir))
        md5 (java.security.MessageDigest/getInstance "MD5")]
    (doseq [f' files]
      (.update md5 (-> (slurp f') (.getBytes "UTF-8"))))
    (BigInteger. 1 (.digest md5))))

(defmulti checksum ext)

(defmethod checksum :coffee
  [f]
  (file-md5 f))

(defmethod checksum :ls
  [f]
  (file-md5 f))

(defmethod checksum :less
  [f]
  (dir-md5 f))

(defn get-kind [file-name]
  (cond (.endsWith file-name ".js") :script
        (.endsWith file-name ".coffee") :coffee
        (.endsWith file-name ".ls") :ls
        (.endsWith file-name ".less") :less
        (.endsWith file-name ".css") :style))

(defn first-existing [files]
  (some #(when (and % (.exists %) (.isFile %)) %) files))

(defn has-suffix
  [req suffix]
  (.endsWith (:uri req) suffix))

(defn style-candidates
  [file-name options]
  (let [file-name (-> file-name
                      (string/replace-first #"\.css$" ".less")
                      (.replaceAll (get options :css-dir "/css")
                                   ""))
        file-path (:less options)
        res-path  (:as-resource options)]
    [(->> file-name (str res-path) io/resource io/as-file)
     (io/as-file (str file-path file-name))]))


(defn script-candidates
  [file-name options]
  (let [file-name (string/replace-first file-name (:js-dir options) "")
        with-ext #(string/replace file-name #"\.js$" (str "." (name %)))
        res-path  (:as-resource options)]
    (flatten (for [f (map with-ext (get options :exts [:coffee :ls]))]
               (let [file-path (options (get-kind f))]
                 [(->> f (str res-path) io/resource io/as-file)
                  (->> f (str file-path) io/as-file)])))))

(defn candidates-for
  [req options]
  (let [file-name (-> (:uri req) codec/url-decode)]
    (if (#{:less :style} (get-kind file-name))
      (style-candidates file-name options)
      (script-candidates file-name options))))

(defn asset-file-for [req options]
  (let [candidates (candidates-for req options)]
    (first-existing candidates)))

(defn is-asset-req [req]
  (and (= :get (:request-method req))
       (or (has-suffix req ".js") ;; TODO - make configurable.
           (has-suffix req ".coffee")
           (has-suffix req ".ls")
           (has-suffix req ".css")
           (has-suffix req ".less"))))

(defonce asset-cache (atom {}))

(defn generate-response [asset-file]
  (case (ext asset-file)
    :coffee {:body (compile-coffeescript asset-file)
             :status 200
             :headers {"Content-Type" "text/javascript"}}
    :ls     {:body (compile-livescript asset-file)
             :status 200
             :headers {"Content-Type" "text/javascript"}}
    :less {:body (less asset-file)
            :status 200
            :headers {"Content-Type" "text/css"}}
    {:status 500 :body "Unknown asset type" :content-type "text/plain"})) ;; Should never happen.

(defn serve-asset [file options]
  (if (:no-cache? options)
    (generate-response file)
    (let [cksm (checksum file)
          cache-record (@asset-cache file)]
      (if (= (:hash cache-record) cksm)
        (do (debug "Serving asset from cache") (:resp cache-record))
        (do
          (debug "Generating new response for" file)
          (let [response (generate-response file)
                cache-record {:hash cksm :resp response}]
            (swap! asset-cache assoc file cache-record)
            response))))))

(defn serve [options req]
  (settings/with-options options
    (when (is-asset-req req)
      (when-let [asset-file (asset-file-for req options)]
        (serve-asset asset-file options)))))

(defn pipeline [& {:keys [strategy] :as options}]
  "Take options and return a function that will wrap a ring handler in an assets pipeline.
   Routes served by the hanlder take precedence. Requests that produce 404 or nil will then
   be processed as asset-requests."
  (let [asset-handler (partial serve options)]
    (case strategy

      :prefer-compiled
      (fn [handler] ;; Handler takes precedence. Allows precompiled assets to take precedence.
        (fn [req]
          (let [response (handler req)]
            (if (or (nil? response) (= 404 (:status response)))
              (or (asset-handler req) response {:status 404})
              response))))

      (fn [handler] ;; Tries to find assets first. Prefers live updates.
        (fn [req]
          (or
            (asset-handler req)
            (handler req)
            {:status 404}))))))

