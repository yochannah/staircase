(ns staircase.tools
  (:use [clojure.string :only (replace-first)]
        [clojure.tools.logging :only (info debug)])
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]))

(def tool-defaults ;; Lots of duplication here - be less explicit? or zen of python?
  {:headingTemplateURI "./heading.html"
   :headingControllerURI "./heading-controller"
   :headlineControllerURI "./controller"
   :headlineTemplateURI "./headline.html"
   :providerURI "./provider"
   :templateURI "./template.html"
   :controllerURI "./controller"
   :width 1})

(defn- fix-uris
  [conf fixer]
  (reduce #(update-in %1 [%2] fixer)
          conf
          [:providerURI :headingTemplateURI :headingControllerURI
           :headlineControllerURI :headlineTemplateURI
           :templateURI :controllerURI :panelHeading :style]))

(defn strip-by-cap
  [conf]
  (let [has-cap (set (:capabilities conf))
        strippers [
                  [#(not (has-cap "next")) [:headingTemplateURI :headingControllerURI]]
                  [#(not (has-cap "provider")) [:providerURI]]
                  [#(and (not (has-cap "initial")) (not (= "native" (:type conf)))) [:templateURI :controllerURI]]]
        reduce-f (fn [c [test-f to-strip]] (if (test-f) (apply dissoc c to-strip) c))]
    (reduce reduce-f conf strippers)))

(defn get-tool-by-name
  [tool-name]
  (with-open [r (-> (str "tools/" (name tool-name) "/tool.json")
                    io/resource
                    io/reader)]
    (-> (slurp r)
        (json/parse-string true)
        (#(merge tool-defaults %))
        (assoc :ident (name tool-name))
        (fix-uris #(when % (replace-first % "." (str "/" (name tool-name)))))
        (strip-by-cap))))

(defn get-tool-conf*
  [tool-def]
  (if (vector? tool-def)
    (let [[tool-name args] tool-def]
      (assoc (get-tool-by-name tool-name) :args args))
    (get-tool-by-name tool-def)))

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
  (->> (:tools config)
        (map get-tool-conf)
        (filter (partial has-capability capability))))

