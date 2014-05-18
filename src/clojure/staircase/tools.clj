(ns staircase.tools
  (:use [clojure.string :only (replace-first)]
        [clojure.tools.logging :only (info debug)])
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]))

(def tool-defaults ;; Lots of duplication here - be less explicit?
  {:headingTemplateURI "./heading.html"
   :headingControllerURI "./heading-controller"
   :providerURI "./provider"
   :templateURI "./template.html"
   :controllerURI "./controller"
   :width 1})

(defn- fix-uris
  [conf fixer]
  (reduce #(update-in %1 [%2] fixer)
          (if (:style conf) (update-in conf [:style] fixer) conf)
          [:providerURI :headingTemplateURI :headingControllerURI :templateURI :controllerURI]))

(defn strip-by-cap
  [conf]
  (let [has-cap (set (:capabilities conf))
        strippers [
                  [#(not (has-cap "next")) [:headingTemplateURI :headingControllerURI]]
                  [#(not (has-cap "provider")) [:providerURI]]
                  [#(and (not (has-cap "initial")) (not (= "native" (:type conf)))) [:templateURI :controllerURI]]]
        reduce-f (fn [c [test-f to-strip]] (if (test-f) (apply dissoc c to-strip) c))]
    (reduce reduce-f conf strippers)))

(defn get-tool-conf*
  [tool-name]
  (with-open [r (-> (str "tools/" (name tool-name) "/tool.json")
                    io/resource
                    io/reader)]
    (-> (slurp r)
        (json/parse-string true)
        (#(merge tool-defaults %))
        (assoc :ident (name tool-name))
        (fix-uris #(replace-first % "." (str "/" (name tool-name))))
        (strip-by-cap))))

;; Don't do io on every access. - but for now in development we should.
(def get-tool-conf get-tool-conf*) ;;(memoize get-tool-conf*))

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

