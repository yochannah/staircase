(ns staircase.handler
  (:use compojure.core
        ring.util.response
        [ring.middleware.session :only (wrap-session)]
        ring.middleware.json
        ring.middleware.format
        ring.middleware.anti-forgery
        [clojure.tools.logging :only (debug info error)]
        [clojure.algo.monads :only (domonad maybe-m)])
  (:require [compojure.handler :as handler]
            [clj-jwt.core  :as jwt]
            [clj-jwt.key   :refer [private-key public-key]]
            [clj-time.core :as t]
            [cheshire.core :as json]
            [clj-http.client :as client]
            [com.stuartsierra.component :as component]
            staircase.resources
            [persona-kit.friend :as pf]
            [persona-kit.core :as pk]
            [persona-kit.middleware :as pm]
            [cemerick.friend :as friend] ;; auth.
            [cemerick.friend.workflows :as workflows]
            [staircase.protocols] ;; Compile, so import will work.
            [staircase.data :as data] ;; Data access routines that don't map nicely to resources.
            [staircase.views :as views] ;; HTML templating.
            [compojure.route :as route]) ;; Standard route builders.
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
        (binding [staircase.resources/context {:user principal}]
          (handler (assoc req ::principal principal)))))))

;; Requires session functionality.
(def app-auth-routes
  (routes
    (GET "/csrf-token" [] (-> (response *anti-forgery-token*) (content-type "text/plain")))
    (POST "/login"
          {session :session :as r}
          (let [ident (friend/identity r)]
            (if (:current ident) (response ident) {:status 403})))
    (friend/logout (POST "/logout"
          {session :session :as r}
          (-> (response "ok") (content-type "text/plain"))))))

;; replacement for persona-kit version. TODO: move to different file.
(defn persona-workflow [audience request]
  (when (and (= (:uri request) "/auth/login")
             (= (:request-method request) :post))
    (-> request
        :params
        (get "assertion")
        (pk/verify-assertion audience)
        pf/credential-fn
        (workflows/make-auth {::friend/redirect-on-auth? false
                              ::friend/workflow :mozilla-persona}))))

;; Route builders

(defn- read-token
  [req]
  (-> (apply merge (map req [:params :form-params :multipart-params]))
      :__anti-forgery-token))

(defn- build-app-routes [router]
  (routes 
    (GET "/" [] (views/index))
    (context "/auth" [] (-> app-auth-routes
                            (wrap-anti-forgery {:read-token read-token})))
    (route/resources "/")
    (route/not-found (views/four-oh-four))))

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
      (wrap-session {:store (:session-store router)})))

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

(defrecord Router [session-store asset-pipeline config secrets histories steps handler]
  component/Lifecycle

  (start [this]
    (info "Starting steps app at" (:audience config))
    (let [auth-conf {:credential-fn pf/credential-fn
                     :workflows [(partial persona-workflow (:audience config))]}
          app-routes (build-app-routes this)
          v1 (context "/api/v1" [] (api-v1 this))
          handler (routes
                      (-> (handler/api v1) wrap-json-body)
                      (-> (handler/api app-routes)
                          (friend/authenticate auth-conf)
                          (wrap-session {:store session-store
                                         :cookie-name "staircase-session"})
                          asset-pipeline
                          pm/wrap-persona-resources))]
      (assoc this :handler (wrap-restful-format handler))))

  (stop [this] this))

(defn new-router [] (map->Router {}))

