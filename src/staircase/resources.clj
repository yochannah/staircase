(ns staircase.resources)

;; Resources can use this dynamic variable for accessing context, such
;; as the identity of the current resource owner.
(def ^:dynamic context {:user nil})

(def owned-resource {:where [:and
                             [:= :id :?uuid]
                             [:= :owner :?user]]})
