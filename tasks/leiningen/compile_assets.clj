(ns leiningen.compile-assets
  (:require [leiningen.core.eval :refer (eval-in-project)]
            [robert.hooke]
            [leiningen.uberjar]))

(def precompile-assets*
  '(do
     (require '[clojure.tools.logging :refer (info)])
     (require '[staircase.tasks.assets :as assets])
     (info "Precompiling assets...")
     (assets/precompile)))

(defn precompile-assets [f & [project :as args]]
  (eval-in-project project precompile-assets*)
  (apply f args))

(defn hooks []
  (robert.hooke/add-hook #'leiningen.uberjar/uberjar
                         precompile-assets))

