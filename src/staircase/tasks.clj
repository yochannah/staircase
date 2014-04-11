(ns staircase.tasks
  (:import [java.io FileNotFoundException])
  (use [clojure.tools.logging :only (info)]
       [clojure.java.io :as io]
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
  (if-let [settings (config :tools)]
    (let [tool-names  (keys settings)
          names       (if (empty? names) tool-names (filter (set names) tool-names))]
      [settings names])
    [{} []]))

;; Pull in all the configured tools.
(defn load-tools [& names] 
  (let [[settings names] (process-args names)]
    (doseq [tool-name names
            :let [tool-settings (settings tool-name)
                  repo-uri  (:uri tool-settings)
                  repo-path (str "tools/" (name tool-name))
                  options   (or (:options tool-settings) {})]]
      (info "Updating" repo-path "from" repo-uri "with options:" (prn-str options))
      (let [repo         (load-or-clone repo-uri repo-path options)
            fetch-result (git-fetch repo)
            git-ref      (or (:ref tool-settings) "master")]
        (git-checkout repo git-ref)))))

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
            :let [repo-path (str "tools/" (name tool-name))]]
      (info "Deleting" repo-path)
      (rm-r repo-path))))

