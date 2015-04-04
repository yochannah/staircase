;; Tasks for managing web-application assets.
;; ------------------------------------------
;; Provides mechanisms for pre-compiling assets, which is helpful in
;; production (especially when uber-warring), as well as a mechanism
;; to delete pre-compiled assets.

(ns staircase.tasks.assets
  (:require [clojure.tools.logging :refer (debug info)]
            [clojure.string :as s]
            [clojure.java.io :as io]
            [staircase.assets :as assets]))

;; Private vars used in public functions.
(declare all-assets ext-for content-type with-new-ext possibly-generated-from compile-asset)

(defn precompile
  "Generate compiled versions of the assets, storing them as they would be accessed."
  []
  (let [compiled (for [f (all-assets)]
                   (do (debug "Compiling" (.getPath f))
                       (compile-asset f)))]
    (info "Success: compiled" (count compiled) "assets")))

(defn clean
  "Delete all the pre-compiled assets, if any."
  []
  (let [deleted (for [asset (all-assets)
                      gen (possibly-generated-from asset)
                      :when (.exists gen)]
                  (do
                    (info "Removing generated file" (.getPath gen))
                    (.delete gen)
                    (.getPath gen)))]
    (info "Success: deleted" (count deleted) "artefacts")))

;; Private functions and vars.

(defn- compile-asset
  "Compile an asset, returning the name of the generated file."
  [f]
  (let [resp (assets/generate-response f)
        content (:body resp)
        new-ext (ext-for (get-in resp content-type))
        new-name (with-new-ext new-ext f)]
    (spit new-name content)
    (info "Wrote" new-name)
    new-name)) ;; return the name we wrote the asset to.

(def ^:private extensions #{"coffee" "less" "ls"})

(defn- extension-of [file]
  (-> (.getName file)
      (s/split #"\.")
      last))

(def ^:private ext-for
  {"text/javascript" "js"
   "text/css" "css"})

(def ^:private content-type [:headers "Content-Type"])

(defn- asset?
  "Determine if a file is a compilable asset"
  [file]
  (and (.isFile file) ;; Ignore directories.
       (not (re-find #"vendor" (.getPath file))) ;; Ignore vendored files.
       (extensions (extension-of file)))) ;; Only assets which we compile.

(defn- find-assets
  "Find all the assets beneath this root"
  [root]
  (->> (io/file root)
       file-seq
       (filter asset?)))

(defn- with-new-ext
  "Get the new filename for this file with the extension changed"
  [ext f]
  (-> (.getPath f)
      (s/replace #"\.\w+$" (str "." ext))))

(def ^:private roots ;; The starting points to look for assets.
  ["src/coffee" "src/less/style.less"
   "resources" "external"])

(defn- all-assets [] (mapcat find-assets roots))

(defn- possibly-generated-from [asset]
  (as-> ["js" "css"] _
    (map #(partial with-new-ext %) _)
    (apply juxt _)
    (_ asset) ;; <-- Inversion of order here, hence as->
    (map io/file _)))
