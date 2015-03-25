(ns staircase.tasks
  (:import [java.io FileNotFoundException])
  (:require [staircase.data :as data]
            [clojure.java.jdbc :as jdbc]
            [staircase.sql :as sql])
  (:use [clojure.tools.logging :only (info warn)]
       [clojure.java.io :as io]
       [staircase.config :only (db-options)]
       [com.stuartsierra.component :only (stop start)]
       [environ.core :only (env)]
       clojure.java.shell
       staircase.config
       clj-jgit.porcelain))

(defn- clone-repo [uri options]
  (git-clone2 uri options)
  (load-repo (:path options)))

(defn- load-or-clone [uri path options]
  (try
    (load-repo path)
    (catch FileNotFoundException fnf (clone-repo uri (assoc options :path path)))))

(defn- process-args [names]
  "Get a list of the configured remote tools."
  (if-let [settings (config :tools)]
    (let [tool-names  (->> (keys settings) (filter #(get-in settings [% :uri])))
          names       (if (empty? names) tool-names (filter (set names) tool-names))]
      [settings names])
    [{} []]))

(def git-tools "external/tools/")

(defn bower-tools []
  (when-let [tools (config :bower-tools)]
    (doseq [[ident uri] tools]
      (println (str (name ident) "=" uri)))))

;; Pull in all the configured tools.
(defn load-tools [& names] 
  (let [[settings names] (process-args names)]
    (doseq [tool-name names
            :let [tool-settings (settings tool-name)
                  repo-uri  (:uri tool-settings)
                  repo-path (str git-tools (name tool-name))
                  options   (or (:options tool-settings) {})]]
      (info "Updating" repo-path "from" repo-uri "with options:" (prn-str options))
      (let [repo         (load-or-clone repo-uri repo-path options)
            fetch-result (git-fetch repo)
            git-ref      (or (:ref tool-settings) "master")]
        (git-checkout repo git-ref)
        (git-pull repo)))))

(defn- rm-r [path]
  (let [f' (fn [f file] ;; y-combinator for non-tail recursion.
             (when (.exists file)
              (when (.isDirectory file)
                (doseq [child (.listFiles file)]
                  (f f child)))
              (io/delete-file file)))]
    (f' f' (io/as-file path))))

(defn clean-tools [& names]
  (let [[settings names] (process-args names)]
    (doseq [tool-name names
            :let [repo-path (str git-tools (name tool-name))]]
      (info "Deleting" repo-path)
      (rm-r repo-path))))

(defn clean-js []
  (info "Deleting node modules")
  (rm-r "node_modules")
  (info "Deleting runtime js dependencies")
  (rm-r "resources/public/vendor"))

(defn js-deps [] ;; I can feel a macro coming on...
  (let [status (sh "npm" "install")]
    (if (= 0 (:exit status))
      (let [status (sh "node_modules/bower/bin/bower" "install")]
        (if (not (= 0 (:exit status)))
          (warn (:err status))))
      (warn (:err status)))))

; Run some bodies within the context of a started component.
(defmacro with-component
  [[started component] & bodies]
  `(let [~started (start ~component)]
    (try (do ~@bodies)
         (finally (stop ~started)))))

(defn run-query [& [query]]
  (if-not query
    (warn "No query!")
    (with-component [db (-> env db-options data/new-pooled-db)]
      (doseq [row (jdbc/query db [query])]
        (prn row)))))

(defn clean-db [& [force?]]
  (if-not (= "force" force?)
    (warn "
      This is a destructive action!
      Pass in `force' to make this go through")
    (let [db-conf (-> env db-options (assoc :migrate false))]
      (with-component [db (data/new-pooled-db db-conf)]
        (sql/drop-all-tables db)))))
