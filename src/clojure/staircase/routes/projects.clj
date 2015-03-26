(ns staircase.routes.projects
  (:require [clojure.tools.logging :refer (info)])
  (:use compojure.core
        staircase.routing
        staircase.protocols
        ring.util.response))

(defn- project-item-routes [projects project-id]
  (routes
    (GET "/:item-id" [item-id]
         (if-let [child (get-child projects project-id item-id)]
           (update-in (response child) [:headers "Link"] conj (str "</api/v1/projects/" project-id ">; rel=\"project\""))
           NOT_FOUND))
    (DELETE "/:itemid" [itemid]
        (if (delete-child projects project-id itemid)
          ACCEPTED
          NOT_FOUND))
    (POST "/" {payload :body}
          (if-let [result (add-child projects project-id payload)]
            (if-let [error (:error result)]
              (assoc CLIENT_ERROR :body (.getMessage error))
              (update-in ACCEPTED [:headers] assoc ;; Ideally we really need a reverse mapper here
                         "Location" (str "/api/v1/projects/" project-id "/items/" result)
                         "X-Entity-ID" (str result)))
            NOT_FOUND)))) ;; returns nil if there is no parent project

(defn link-project
  [{id :id parent :parent_id items :contents subprojs :child_nodes}]
  (concat
    (if parent [[(str "/api/v1/projects/" parent) "project"]] [])
    (map #(vector (str "/api/v1/projects/" id "/items/" (:id %)) "item") items)
    (map #(vector (str "/api/v1/projects/" (:id %)) "subproject") subprojs)))

(defn- project-routes [projects id]
  (routes
    (GET "/" []
         (get-resource projects id :linker link-project))
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
