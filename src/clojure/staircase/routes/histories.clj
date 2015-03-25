(ns staircase.routes.histories
  (:use compojure.core
        staircase.routing
        staircase.protocols
        [clojure.algo.monads :only (domonad maybe-m)]
        ring.util.response)
  (:require
        [staircase.resources.histories :as hs]))

(defn get-end-of-history [histories id]
  (or
    (when-let [end (first (hs/get-steps-of histories id :limit 1))]
      (response end))
    NOT_FOUND))

(defn get-steps [histories id]
  (or
    (when (exists? histories id)
      (response (into [] (hs/get-steps-of histories id))))
    NOT_FOUND))

(defn get-step-of [histories id idx]
  (or
    (domonad maybe-m ;; TODO: use offsetting rather than nth.
             [:when (exists? histories id)
              i     (try (Integer/parseInt idx)
                         (catch NumberFormatException e nil))
              step  (nth (hs/get-steps-of histories id) i)]
          (response step))
    NOT_FOUND))

(defn fork-history-at [histories id idx body]
  (or
    (domonad maybe-m
             [original (get-one histories id)
              title (or (get body "title")
                        (str "Fork of " (:title original)))
              history (-> original
                          (dissoc :id :steps)
                          (assoc :title title))
              ;; Don't really have to nummify it,
              ;; but is good to catch error here and prevent
              ;; doing useless work downstream.
              limit (try (Integer/parseInt idx)
                         (catch NumberFormatException e nil))
              inherited-steps (hs/get-history-steps-of histories id limit)
              hid (create histories history)]
             (do
              (hs/add-all-steps histories hid inherited-steps)
              (response (get-one histories hid))))
    NOT_FOUND))

(defn add-step-to [histories steps id doc]
  (if (exists? histories id)
    (let [to-insert (assoc doc "history_id" id)
          step-id (create steps to-insert)]
      (response (get-one steps step-id)))
    NOT_FOUND))

(defn build-hist-routes [{{:keys [histories steps]} :resources}]
  (routes ;; routes that start from histories.
          (GET  "/" [] (get-resources histories))
          (POST "/" {body :body} (create-new histories body))
          (context "/:id" [id]
                   (GET    "/" [] (get-resource histories id))
                   (PUT    "/" {body :body} (update-resource histories
                                                             id
                                                             (dissoc body "id" "steps" "owner")))
                   (DELETE "/" [] (delete-resource histories id))
                   (GET    "/head" [] (get-end-of-history histories id))
                   (context "/steps" []
                            (GET "/" [] (hs/get-steps-of histories id))
                            (GET "/:idx" [idx] (get-step-of histories id idx))
                            (POST "/:idx/fork"
                                  {body :body {idx :idx} :params}
                                  (fork-history-at histories id idx body))
                            (POST "/" {body :body} (add-step-to histories steps id body))))))
