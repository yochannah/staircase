(ns staircase.routes.projects
  (:use compojure.core
        staircase.routing
        staircase.protocols
        ring.util.response))

(defn- project-item-routes [projects project-id]
  (routes
    (DELETE "/:itemid" [itemid]
        (if (delete-child projects project-id itemid)
          ACCEPTED
          NOT_FOUND))
    (POST "/" {payload :body}
          (if (add-child projects project-id payload)
            ACCEPTED
            NOT_FOUND))))

(defn- project-routes [projects id]
  (routes
    (GET "/" [] (get-resource projects id))
    (DELETE  "/" [] (delete-resource projects id))
    (PUT  "/" {payload :body}
         (update-resource projects id payload))
    (context "/items" []
             (project-item-routes projects id))))

(defn build-project-routes [{{:keys [projects]} :resources}]
  (routes ;; routes that start from projects
          (GET  "/" []
               (get-resources projects))
          (POST "/" {payload :body}
                (create-new projects payload))
          (context "/:id" [id]
                (project-routes projects id))))
