(ns staircase.handler
  (:import com.mchange.v2.c3p0.ComboPooledDataSource)
  (:use compojure.core)
  (:use cheshire.core)
  (:use ring.util.response)
  (:require [compojure.handler :as handler]
            [ring.middleware.json :as middleware]
            [clojure.java.jdbc :as sql]
            [compojure.route :as route]))

(def db-config
  {:classname "org.h2.Driver"
    :subprotocol "h2"
    :subname "mem:histories"
    :user ""
    :password ""})

(defn pool
      [config]
      (let [cpds (doto (ComboPooledDataSource.)
                   (.setDriverClass (:classname config))
                   (.setJdbcUrl (str "jdbc:" (:subprotocol config) ":" (:subname config)))
                   (.setUser (:user config))
                   (.setPassword (:password config))
                   (.setMaxPoolSize 6)
                   (.setMinPoolSize 1)
                   (.setInitialPoolSize 1))]
        {:datasource cpds}))

(def pooled-db (delay (pool db-config)))

(defn db-connection [] @pooled-db)

(sql/with-connection (db-connection)
  (sql/create-table :histories [:id "varchar(256)" "primary key"]
                               [:title "varchar(1024)"]
                               [:text :varchar])
  (sql/create-table :history-step
                    [:history_id "varchar(256)"]
                    [:created_at "datetime"]
                    [:step_id "varchar(256)"])
  (sql/create-table :steps [:id "varchar(256)" "primary key"]
                           [:title "varchar(1024)"]
                           [:tool "varchar(1024)"]
                           [:data :varchar]))

(defn find-history [id]
  (sql/with-connection (db-connection)
    (sql/with-query-results results
      ["select h.*, hs.step_id from histories as h, history-step as hs where h.id = ? and h.id = hs.history_id" id]
      (when results (apply build-history results)))))

(defn build-history [row & rows]
  (let [init (assoc row :steps (into [] (:step_id row)))
        add-step (fn [h row] (update-in h [:steps] conj (:step_id row)))]
    (reduce add-step init rows)))

(defn get-history [id]
   (if-let [history (find-history id)]
     (response history)
     {:status 404}))

(defn get-all-histories []
  (response (sql/with-connection (db-connection)
    (sql/with-query-results results
      ["select * from histories"]
      (into [] results)))))

(defn uuid [] (str (java.util.UUID/randomUUID)))

(defn create-new-history [doc]
  (let [id (uuid)
        document (assoc doc "id" id)]
    (sql/with-connection (db-connection)
      (sql/insert-record :histories document))
    (get-history id)))

(defn update-history [id doc]
  (sql/with-connection (db-connection)
    (let [document (assoc doc "id" id)]
      (sql/update-values :histories ["id=?" id] document)))
  (get-history id))

(defn delete-history [id]
  (if (find-history id)
    (do (sql/with-connection (db-connection)
      (sql/delete-rows :histories ["id=?" id]))
      {:status 204})
    {:status 404}))

(defn get-end-of-history [id]
  (sql/with-connection (db-connection)
    (sql/with-query-results results
      ["select s.* from steps as s, history-step as hs where hs.history_id = ?" id]
      (if (results)
        (response (first results))
        {:status 404}))))

(defroutes app-routes
  (GET "/" [] "Hello World")
  (route/resources "/")
  (route/not-found "Not Found"))

(defroutes api-routes
  (context "/histories" [] (defroutes histories-routes
            (GET  "/" [] (get-all-histories))
            (POST "/" {body :body} (create-new-history body))
            (context "/:id" [id] (defroutes history-routes
                      (GET    "/" [] (get-history id))
                      (GET    "/head" [] (get-end-of-history id))
                      (PUT    "/" {body :body} (update-history id body))
                      (DELETE "/" [] (delete-history id)))
                      (context "/steps" (defroutes history-steps-routes
                               (GET "/" [] (get-steps-of id))
                               (GET "/:idx" [idx] (get-step-of id idx))
                               (POST "/" {body :body} (add-step-to id body)))))))
                      
  (context "/steps" [] (defroutes steps-routes
            (GET  "/" [] (get-all-steps))
            (context "/:id" [id] (defroutes step-routes
                      (GET    "/" [] (get-step id))
                      (DELETE "/" [] (delete-step id)))))))

(def app
  (routes
    (-> (handler/api api-routes)
        (middleware/wrap-json-body)
        (middleware/wrap-json-response))
    (handler/site app-routes)))
