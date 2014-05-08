(ns staircase.tools
  (:use [clojure.string :only (replace-first)]
        [clojure.tools.logging :only (info debug)])
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]))

(def tool-defaults
  {:headingURI "./heading.html"
   :templateURI "./template.html"
   :controllerURI "./controller.js"
   :width 1})

(defn get-tool-conf
  [tool-name]
  (with-open [r (-> (str "tools/" (name tool-name) "/tool.json")
                    io/resource
                    io/reader)]
    (let [prefix #(replace-first % "." (str "/" (name tool-name)))]
      (-> (slurp r)
          (json/parse-string true)
          (#(merge tool-defaults %))
          (assoc :ident (name tool-name))
          (update-in [:headingURI] prefix)
          (update-in [:templateURI] prefix)
          (update-in [:controllerURI] prefix)))))

(defn has-capability
  [capability tool-conf]
  (if capability
    ((set (:capabilities tool-conf)) capability)
    true))

(defn get-tool
  [config ident]
  (first (filter #(= ident (:ident %)) (map get-tool-conf (:tools config)))))

(defn get-tools
  [config capability]
  (debug "Getting tools with the" capability "capability")
  (let [tools (config :tools)]
    (->> tools
         (map get-tool-conf)
         (filter (partial has-capability capability)))))

