(ns staircase.handler
  (:use compojure.core)
  (:use cheshire.core)
  (:use ring.util.response)
  (:use [clojure.tools.logging :only (debug info error)])
  (:require [compojure.handler :as handler]
            [com.stuartsierra.component :as component]
            [staircase.data :as data]
            [staircase.config]
            [ring.middleware.json :as middleware]
            [clojure.java.jdbc :as sql]
            [compojure.route :as route])
  (:import staircase.data.Resource)
  (:import com.mchange.v2.c3p0.ComboPooledDataSource))

(def conf (staircase.config/config (or (System/getenv "ENVIRONMENT") :development)))

(info "Config:" conf)

(def NOT_FOUND {:status 404})
(def ACCEPTED {:status 204})

(defn get-resource [rs id]
   (if-let [ret (.get-one rs id)]
     (response ret)
     NOT_FOUND))

(defn get-resources [rs]
  (response (.get-all rs)))

(defn create-new [rs doc]
  (let [id (.create rs doc)]
    (get-resource rs id)))

(defn update-resource [rs id doc]
  (if (.exists? rs id)
    (response (.update rs id doc))
    NOT_FOUND))

(defn delete-resource [rs id]
  (if (.exists? rs id)
    (do 
      (.delete rs id)
      ACCEPTED)
    NOT_FOUND))

(defn get-end-of-history [histories id]
  (or
    (when-let [end (first (data/get-steps-of histories id :limit 1))]
      (response end))
    NOT_FOUND))

(defn get-steps-of [histories id]
  (or
    (when (.exists? histories id)
      (response (data/get-steps-of histories id)))
    NOT_FOUND))

(defn get-step-of [histories id idx]
  (or
    (when (.exists? histories id)
      (when-let [step (first (reduce conj '() (data/get-steps-of histories id)))]
        (response step)))
    NOT_FOUND))

(defn add-step-to [histories steps id doc]
  (if (.exists? histories id)
    (let [to-insert (assoc doc "history_id" id)
          step-id (.create steps to-insert)]
      (response (.get-one steps step-id)))
    NOT_FOUND))

(defrecord App [histories steps db handler]
  component/Lifecycle

  (start [app]
    (let [app-routes (routes 
                        (GET "/" [] "Hello World")
                        (route/resources "/")
                        (route/not-found "Not Found"))
          hist-routes (routes
                (GET  "/" [] (get-resources histories))
                (POST "/" {body :body} (create-new histories body))
                (context "/:id" [id]
                  (GET    "/" [] (get-resource histories id))
                  (GET    "/head" [] (get-end-of-history histories id))
                  (PUT    "/" {body :body} (update-resource histories id body))
                  (DELETE "/" [] (delete-resource histories id))
                  (context "/steps" []
                          (GET "/" [] (get-steps-of histories id))
                          (GET "/:idx" [idx] (get-step-of histories id idx))
                          (POST "/" {body :body} (add-step-to histories steps id body)))))
          step-routes (routes
                (GET  "/" [] (get-resources steps))
                (context "/:id" [id]
                          (GET    "/" [] (get-resource steps id))
                          (DELETE "/" [] (delete-resource steps id))))
          api-routes (routes (context "/histories" [] hist-routes) (context "/steps" [] step-routes))
          handler (routes (-> (handler/api api-routes)
                            (middleware/wrap-json-body)
                            (middleware/wrap-json-response))
                          (handler/site app-routes))]
      (assoc app :handler handler)))

  (stop [app] app))

(defn new-app [] (map->App {}))

;; Inject dependencies and build up the system.
(defn build-system [options]
  (component/start
    (let [{:keys [db]} options]
      (-> (component/system-map
            :db (data/new-pooled-db db)
            :histories (data/new-history-resource)
            :steps (data/new-steps-resource)
            :app (new-app))
          (component/system-using
            {:app [:db :histories :steps]
            :steps [:db]
            :histories [:db]})))))

(def app (get-in (build-system conf) [:app :handler]))

