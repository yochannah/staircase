(ns staircase.routes.service
  (:use compojure.core
        staircase.protocols
        staircase.routing
        staircase.helpers
        ring.util.response)
  (:require
            [clojure.tools.logging :refer (info debug)]
            [devlin.table-utils :refer (full-outer-join)]
            [clj-http.client :as client]
    ))

(defn register-for ;; currently gets anon session. Needs api hf to be applied.
  [service]
  (let [http-conf {:as :json :throw-exceptions false}
        token-coords [:body :token]
        session-url  (str (:root service) "/session")]
    (debug "Registering for" service session-url)
    (-> session-url
        (client/get http-conf)
        (get-in token-coords))))

(defn ensure-name [service]
  (-> service (assoc :name (or (:name service) (:confname service))) (dissoc :confname)))

(defn ensure-token [services service]
  (if (and (:token service) (:valid_until service) (.before (now) (:valid_until service)))
    service
    (let [token (register-for service)
          current (first (get-where services [:= :root (:root service)]))
          canon (if current
                  (update services (:id current) {:token token})
                  (get-one services (create services
                                            {:name (:name service)
                                             :root (:root service)
                                             :token token})))]
      (merge service canon))))

(defn- ensure-valid [services service]
  (->> service ensure-name (ensure-token services)))

(defn- get-services
  [services config]
  (locking services ;; Not very happy about this - is there some better way to avoid this bottle-neck?
    (letfn [(as-service [[k v]] {:root v
                                  :confname k
                                  :meta (get-in config [:service-meta k])})]
      (let [user-services (get-all services)
            configured-services (->> config
                                     :services
                                     (map as-service))
            joined-services (full-outer-join configured-services user-services :root)]
        (response (vec (map (partial ensure-valid services) joined-services)))))))

(defn- delete-service [services ident]
  (if-let [id (-> services (get-where [:= :name ident]) first :id)]
    (do (delete services id) SUCCESS)
    NOT_FOUND))

(defn- get-service [services config ident]
  (locking services
    (let [uri (get-in config [:services ident])
          user-services (get-where services [:= :root uri])
          service (-> [{:root uri :name ident}]
                      (full-outer-join user-services :root)
                      first)]
      (response (ensure-valid services service)))))

;; Create-or-update semantics
(defn- put-service [services config ident doc]
  (locking services
    (let [uri (or (:root (first (get-where services [:= :name ident]))) ;; The user's service.
                  (get-in config [:services ident]))            ;; Default configured service.
          current (first (get-where services [:= :root uri]))
          new-root (get doc "root")]
      (cond
        (and current (or (nil? new-root) (= (:root current) new-root)))
          (update services (:id current) doc)
        (and current new-root) ;; We are updating the root - also set the token.
          (update services (:id current) (assoc doc :token (register-for {:root new-root})))
        :else
          (try
            (let [token (or (get doc "token")
                            (register-for {:root uri}))] ;; New record. Ensure valid.
              (create-new services (-> doc (assoc :root uri :token token) (dissoc "root" "token"))))
            (catch Exception e
              {:status 400
               :body {:message (str "bad service definition: " e)}
               }))))))

(defn build-service-routes [{{:keys [services]} :resources config :config}]
  (let [real-id #(if (= "default" %) (:default-service @config) %)]
    (routes ;; Routes for getting service information.
            (GET "/" []
                 (get-services services @config))
            (context "/:ident" [ident]
                     (GET "/" []
                          (get-service services @config (real-id ident)))
                     (DELETE "/" []
                             (delete-service services @config (real-id ident)))
                     (PUT "/" {doc :body}
                          (put-service services @config (real-id ident) doc))))))
