(ns staircase.handler
  (:use compojure.core
        ring.util.response
        [clojure.tools.logging :only (debug info error)]
        [clojure.algo.monads :only (domonad maybe-m)])
  (:require [compojure.handler :as handler]
            [clj-jwt.core  :as jwt]
            [clj-jwt.key   :refer [private-key public-key]]
            [clj-time.core :as t]
            [cheshire.core :as json]
            [clj-http.client :as client]
            [com.stuartsierra.component :as component]
            ring.middleware.json
            ring.middleware.session
            [staircase.protocols] ;; Compile, so import will work.
            [staircase.data :as data]
            [compojure.route :as route])
  (:import staircase.protocols.Resource))

(def NOT_FOUND {:status 404})
(def ACCEPTED {:status 204})

(defn get-resource [rs id]
  (if-let [ret (.get-one rs id)]
    (response ret)
    NOT_FOUND))

;; Have to vectorise, since lazy seqs won't be jsonified.
(defn get-resources [rs] (response (into [] (.get-all rs))))

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
      (response (into [] (data/get-steps-of histories id))))
    NOT_FOUND))

(defn get-step-of [histories id idx]
  (or
    (domonad maybe-m
             [:when (.exists? histories id)
              i     (try (Integer/parseInt idx) (catch NumberFormatException e nil))
              step  (nth (data/get-steps-of histories id) (Integer/parseInt idx))]
          (response step))
    NOT_FOUND))

(defn add-step-to [histories steps id doc]
  (if (.exists? histories id)
    (let [to-insert (assoc doc "history_id" id)
          step-id (.create steps to-insert)]
      (response (.get-one steps step-id)))
    NOT_FOUND))

(defn logout ;; Delete session, and send user somewhere.
  ([] (logout "/"))
  ([send-to] 
    (-> (redirect send-to) (assoc :session nil))))

(defn verify-persona-assertion [session verifier audience assertion]
  (let [resp (client/post verifier {:form-params {:audience audience :assertion assertion} :throw-exceptions false})
        status (:status resp)]
    (if (not (= status 200))
      {:status 500 :session (dissoc session :identity)}
      (let [{email :email} (json/parse-string (:body resp))
            session (assoc session :identity email)]
        (-> (response "ok") (assoc :session session))))))

(defn- issue-session [config secrets ident]
  (let [claim {:iss (:whoami config)
              :exp (t/plus (t/now) (t/days 1))
              :prn ident}
        rsa-prv-key (private-key "rsa/private.key" (:key-phrase secrets))]
    (-> claim jwt/jwt (jwt/sign :RS256 rsa-prv-key) jwt/to-str)))

(defn- get-principal [router auth]
  (if (and auth (.startsWith auth "Token: "))
    (let [token (.replace auth "Token: " "")
          rsa-pub-key (public-key  "rsa/public.key")
          web-token (jwt/str->jwt token)
          valid? (jwt/verify web-token)
          claims (:claims web-token)]
      (if (and valid? (t/after? (:exp claims) (t/now)))
        (:prn claims)
        ::invalid))
    nil))

(defn- wrap-api-auth [handler router]
  (fn [req]
    (let [{{auth :Authorization} :headers} req
          principal (get-principal router auth)]
      (if (= ::invalid principal)
        {:status 401 :body {:message "Bad authorization."}}
        (handler (assoc req ::principal principal))))))

;; Route builders

(defn- build-app-routes [router]
  (routes 
    (GET "/" [] "Hello World")
    (POST "/logout" [send-to] (logout send-to))
    (POST "/verify"
          {{assertion :assertion} :params session :session}
          (let [{:keys [audience verifier]} (:config router)]
            (verify-persona-assertion session verifier audience assertion)))
    (route/resources "/")
    (route/not-found "Not Found")))

(defn- build-hist-routes [{:keys [histories steps]}]
  (routes ;; routes that start from histories.
          (GET  "/" [] (get-resources histories))
          (POST "/" {body :body} (create-new histories body))
          (context "/:id" [id]
                   (GET    "/" [] (get-resource histories id))
                   (PUT    "/" {body :body} (update-resource histories id body))
                   (DELETE "/" [] (delete-resource histories id))
                   (GET    "/head" [] (get-end-of-history histories id))
                   (context "/steps" []
                            (GET "/" [] (get-steps-of histories id))
                            (GET "/:idx" [idx] (get-step-of histories id idx))
                            (POST "/" {body :body} (add-step-to histories steps id body))))))

(defn build-step-routes [{:keys [steps]}]
  (routes ;; routes for access to step data.
          (GET  "/" [] (get-resources steps))
          (context "/:id" [id]
                   (GET    "/" [] (get-resource steps id))
                   (DELETE "/" [] (delete-resource steps id)))))

(defn- build-api-session-routes [router]
  (-> (routes
        (POST "/" {sess :session} (issue-session (:config router) (:secrets router) (:identity sess))))
      (ring.middleware.session/wrap-session {:store (:session-store router)})))

(defn- api-v1 [router]
  (let [hist-routes (build-hist-routes router)
        step-routes (build-step-routes router)
        api-session-routes (build-api-session-routes router)]
    (routes ;; put them all together
            (context "/sessions" [] api-session-routes)
            (-> (routes 
                  (context "/histories" [] hist-routes)
                  (context "/steps" [] step-routes))
                (wrap-api-auth router))
            (route/not-found {:message "Not found"}))))

(defrecord Router [session-store config secrets histories steps handler]
  component/Lifecycle

  (start [this]
    (let [app-routes (build-app-routes this)
          v1 (context "/api/v1" [] (api-v1 this))
          handler (routes (-> (handler/api v1)
                              (ring.middleware.json/wrap-json-body)
                              (ring.middleware.json/wrap-json-response))
                          (handler/site app-routes {:session {:store session-store}}))]
      (assoc this :handler handler)))

  (stop [this] this))

(defn new-router [] (map->Router {}))

