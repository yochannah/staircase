(ns staircase.handler
  (:use compojure.core
        ring.util.response
        [ring.middleware.session :only (wrap-session)]
        [ring.middleware.cookies :only (wrap-cookies)]
        [ring.middleware.keyword-params :only (wrap-keyword-params)]
        [ring.middleware.nested-params :only (wrap-nested-params)]
        [ring.middleware.params :only (wrap-params)]
        [ring.middleware.basic-authentication :only (wrap-basic-authentication)]
        ring.middleware.json
        ring.middleware.anti-forgery
        staircase.routing ;; small helpers.
        [staircase.protocols]
        [staircase.helpers :only (new-id)]
        [clojure.tools.logging :only (debug info error)]
        [clojure.algo.monads :only (domonad maybe-m)])
  (:require [compojure.handler :as handler]
            [devlin.table-utils :refer (full-outer-join)]
            [clj-time.core :as t]
            [cheshire.core :as json]
            [com.stuartsierra.component :as component]
            [ring.middleware.cors :refer (wrap-cors)]
            [ring.middleware.format :refer (wrap-restful-format)]
            staircase.resources
            [staircase.routes.projects :refer (build-project-routes)]
            [staircase.routes.service :refer (build-service-routes)]
            [staircase.routes.histories :refer (build-hist-routes)]
            [persona-kit.friend :as pf]
            [persona-kit.core :as pk]
            [persona-kit.middleware :as pm]
            [cemerick.drawbridge :as drawbridge]
            [cemerick.friend :as friend] ;; auth.
            [cemerick.friend.workflows :as workflows]
            [staircase.tools :refer (get-tools get-tool get-categories)]
            [staircase.data :as data] ;; Data access routines that don't map nicely to resources.
            [staircase.views :as views] ;; HTML templating.
            [compojure.route :as route]
            [staircase.tokens :refer (issue-session valid-claims)])
  )

(defn get-principal [router auth]
  (or
    (when (and auth (.startsWith auth "Token: "))
      (let [token (.replace auth "Token: " "")]
        (:prn (valid-claims (:secrets router) token))))
    ::invalid))

(defn- wrap-api-auth [handler router]
  (fn [req]
    (let [auth (get-in req [:headers "authorization"])
          principal (get-principal router auth)]
      (if (= ::invalid principal)
        {:status 403 :body {:message "Bad authorization."}}
        (binding [staircase.resources/context {:user principal}]
          (handler (assoc req ::principal principal)))))))

;; Requires session functionality.
(defn app-auth-routes [{:keys [config secrets]}]
  (let [get-session (partial issue-session @config secrets)
        session-resp #(-> %
                          get-session
                          response
                          (content-type "application/json-web-token"))]
    (routes
      (GET "/csrf-token" [] (-> (response *anti-forgery-token*) (content-type "text/plain")))
      (GET "/session"
           {session :session :as r}
           (assoc (session-resp (:current (friend/identity r))) :session session))
      (POST "/login"
            {session :session :as r}
            (if (:email (friend/current-authentication r))
              (assoc (response (friend/identity r)) :session session) ;; Have to record session here.
              {:status 403}))
      (friend/logout (POST "/logout"
                           {session :session :as r}
                           (-> (response "ok")
                               (assoc :session nil)
                               (content-type "text/plain")))))))

;; replacement for persona-kit version. TODO: move to different file.
(defn persona-workflow [audience request]
  (if (and (= (:uri request) "/auth/login")
             (= (:request-method request) :post))
    (-> request
        :params
        (get "assertion")
        (pk/verify-assertion audience)
        pf/credential-fn
        (workflows/make-auth {::friend/redirect-on-auth? false
                              ::friend/workflow :mozilla-persona}))))

(defn anonymous-workflow [request]
  "Assign an identity if none is available."
  (if-not (:current (friend/identity request))
    (workflows/make-auth {:anon? true :identity (str (new-id))}
                         {::friend/redirect-on-auth? false
                          ::friend/workflow :anonymous})))

(defn credential-fn
  [auth]
  (case (::friend/workflow auth)
    :anonymous       (assoc auth :roles [])
    :mozilla-persona (assoc (pf/credential-fn auth) :roles [:user])))

(defn- read-token
  [req]
  (-> (apply merge (map req [:params :form-params :multipart-params]))
      :__anti-forgery-token))

(def drawbridge-handler
  (-> (drawbridge/ring-handler)
      wrap-keyword-params
      wrap-nested-params
      wrap-params
      wrap-session))

(defn- wrap-drawbridge [handler user-creds]
  (letfn [(repl?           [req] (and (= "/repl" (:uri req)) (not-any? nil? user-creds)))
          (authenticated?  [usr pwd] (= [usr pwd] user-creds))]
    (fn [req]
      (let [repl (wrap-basic-authentication drawbridge-handler authenticated?)
            h    (if (repl? req) repl handler)]
        (h req)))))

(defn- build-app-routes [{conf :config :as app}]
  (let [serve-index #(views/index @conf)
        greedy #".+"]
    (routes
      (GET "/" [] (serve-index))
      (GET "/about" [] (serve-index))
      (GET "/projects" [] (serve-index))
      (GET ["/projects/:path" :path greedy] [] (serve-index))
      (GET "/history/:id/:idx" [] (serve-index))
      (GET "/starting-point/:tool" [] (serve-index))
      (GET "/starting-point/:tool/:service" [] (serve-index))
      (GET ["/starting-point/:tool/:service/:args" :args greedy] [] (serve-index))
      (GET "/tools" [capabilities] (response (get-tools @conf capabilities)))
      (GET "/tool-categories" [] (response (get-categories @conf)))
      (GET "/tools/:id" [id] (if-let [tool (get-tool @conf id)]
                              (response tool)
                              {:status 404}))
      (GET "/partials/:fragment.html"
          [fragment]
          (views/render-partial @conf fragment))
      (context "/auth" [] (-> (app-auth-routes app)
                              (wrap-anti-forgery {:read-token read-token})))
      (route/resources "/" {:root "tools"})
      (route/resources "/" {:root "public"})
      (route/not-found (views/four-oh-four @conf)))))

(defn build-step-routes [{{:keys [steps]} :resources}]
  (routes ;; routes for access to step data.
          (GET  "/" [] (get-resources steps))
          (context "/:id" [id]
                   (GET    "/" [] (get-resource steps id))
                   (DELETE "/" [] (delete-resource steps id)))))

;; Routes delivering dynamic config to the client.
(defn build-config-routes
      [{:keys [config]}]
      (routes (GET "/" [] (response (:client @config)))))

(defn- now [] (java.util.Date.))

(defn- build-api-session-routes [{:keys [config secrets session-store]}]
  (-> (routes
        (POST "/"
              {sess :session}
              (issue-session @config secrets (:identity sess))))
      (wrap-session {:store session-store})))

(defn wrap-exception-summaries [handler {config :config}]
  (fn [req]
    (let [resp (handler req)]
      (if (and (= 400 (:status resp))
               (get-in resp [:body :type]))
        (let [summary (get-in @config [:exceptions (:body resp)])]
          (update-in resp [:body :summary] (constantly summary)))
        resp))))

(defn- api-v1 [router]
  (let [hist-routes        (build-hist-routes router)
        step-routes        (build-step-routes router)
        project-routes     (build-project-routes router)
        service-routes     (build-service-routes router)
        api-session-routes (build-api-session-routes router)
        config-routes      (build-config-routes router)]
    (routes ;; put them all together
            (context "/sessions" [] api-session-routes) ;; Getting tokens
            (context "/client-config" [] config-routes) ;; Getting config
            (-> (routes ;; Protected resources.
                  (context "/histories" [] hist-routes)
                  (context "/services" [] service-routes)
                  (context "/steps" [] step-routes)
                  (context "/projects" [] project-routes))
                (wrap-api-auth router)
                (wrap-exception-summaries router))
            (route/not-found {:message "Not found"}))))

(defn wrap-bind-user
  [handler]
  (fn [request]
    (let [user (:identity (friend/current-authentication request))]
      (binding [staircase.resources/context {:user user}]
        (handler request)))))

;; Move to config! TODO
(def friendly-hosts [ #"http://localhost:" ;; iframe tools will not be allowed to access fonts if the iframe source is not configure in this list
                                           ;; the primary symptom of this is crazy icons which are defaulting to system wierdness, and cors font
                                           ;; errors in the console.
                     #"http://[^/]*"
                     #"http://[^/]*labs.intermine.org"
                     #"http://intermine.github.io"
                     #"http://intermine-tools.github.io"
                     #"http://alexkalderimis.github.io"])

(defn allowed-origins
  [audience]
  (if audience (conj friendly-hosts (re-pattern audience)) friendly-hosts))

(defrecord Router [session-store
                   middlewares
                   config
                   secrets
                   resources
                   handler]

  component/Lifecycle

  (start [this]
    (info "Starting steps app at" (:audience @config))
    (let [persona (partial persona-workflow (:audience @config))
          auth-conf {:credential-fn credential-fn
                     :workflows [persona anonymous-workflow]}
          repl-user-creds (map #(% @config) [:repl-user :repl-pwd]) ;; Do not allow these to change.
          app-routes (build-app-routes this)
          v1 (context "/api/v1" [] (api-v1 this))
          handler (routes
                      (-> (handler/api v1) wrap-json-body)
                      (-> app-routes
                          handler/api
                          (friend/authenticate auth-conf)
                          (wrap-session {:store session-store})
                          (wrap-cookies)
                          ((apply comp identity middlewares))
                          pm/wrap-persona-resources))
          app (-> handler
                  (wrap-restful-format :formats [:json :edn])
                  (wrap-cors :access-control-allow-origin (allowed-origins (:audience config)))
                  (wrap-drawbridge repl-user-creds))]
      (assoc this :handler app)))

  (stop [this] this))

(defn new-router
  "Create a new router"
  []
  (map->Router {:middlewares []}))
