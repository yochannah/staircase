(ns staircase.tools
  (:use [clojure.string :only (replace-first)])
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]))

(def tool-defaults
  {:templateURI "./template.html"
   :controllerURI "./controller.js"
   :width 1})

(defn get-tool-conf
  [tool-name]
  (with-open [r (-> (str "tools/" (name tool-name) "/tool.json")
                    io/resource
                    io/reader)]
    (let [prefix #(replace-first % "." (name tool-name))]
      (-> (slurp r)
          (json/parse-string true)
          (#(merge tool-defaults %))
          (assoc :ident (name tool-name))
          (update-in [:templateURI] prefix)
          (update-in [:controllerURI] prefix)))))

(defn has-capability
  [capability tool-conf]
  (if capability
    ((set (:capabilities tool-conf)) capability)
    true))

(defn get-tools
  [config capability]
  (let [tools (config :tools)]
    (->> tools
         (map get-tool-conf)
         (filter (partial has-capability capability)))))

