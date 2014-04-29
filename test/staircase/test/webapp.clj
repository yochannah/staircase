(ns staircase.test.webapp
  (:require [cheshire.core :as json]
            [persona-kit.core :as pk]
            [ring.middleware.anti-forgery :as af]
            [staircase.data :as data]
            [clj-jwt.core :as jwt]
            [ring.middleware.session.memory :refer (memory-store)]
            [com.stuartsierra.component :as component])
  (:use clojure.test
        clojure.tools.logging
        staircase.protocols
        staircase.test.util
        peridot.core
        staircase.handler)
  (import staircase.test.util.MockResource))

(defn- verify-assertion
  [status assertion audience]
  {:status status :email assertion :audience audience})

(defn json-payload
  [data]
  (-> (json/generate-string data)
      (.getBytes "UTF-8")
      clojure.java.io/input-stream))

(defn login-payload
  [ident csrf]
  (json-payload  {:assertion ident
                  :__anti-forgery-token csrf}))

(defn current-user
  [response]
  (-> response :body slurp json/parse-string (get "current")))

(defn as-prn
  [response]
  (-> response :body jwt/str->jwt :claims :prn))

(defn ws-headers
  [token]
  {"authorization" (str "Token: " token)
   "accept" "application/json"})

(defn ws-post
  [session uri token payload]
  (request session uri
           :request-method :post
           :body (json-payload payload)
           :content-type "application/json"
           :headers (ws-headers token)))

(defn ws-get
  [session uri token]
  (request session uri
           :request-method :get
           :headers (ws-headers token)))

(def ^:dynamic app nil)

(defn setup [t]
  (let [histories (atomic-resource (map #(hash-map "id" %1 "owner" "foo") (range 3)))
        steps     (atomic-resource (map #(hash-map "id" %1 "owner" "foo") (range 5)))]
    (binding [app (get-router histories steps)]
      (t))))

(use-fixtures :each setup)

(deftest test-main-route
    (let [{:keys [response]} (-> (session app)
                        (request "/"))
          status (:status response)
          content-type (get-in response [:headers "Content-Type"])]
      (is (= status 200))
      (is (= content-type "text/html; charset=utf-8"))))

(deftest test-csrf-tokens
    (let [sess (-> (session app)
                    (request "/auth/csrf-token"))
          csrf (get-in sess [:response :body])
          sess (request sess "/auth/csrf-token")
          csrf' (get-in sess [:response :body])]
    (testing "csrf tokens are stable within a session"
      (is (and csrf csrf'))
      (is (= csrf csrf')))))

(deftest test-logging-in
  (testing "success"
    ;; stub out the callback to persona.
    (with-redefs [pk/verify-assertion (partial verify-assertion "okay")]
      (let [ident "foo@bar.org"
            sess (-> (session app)
                      (request "/")
                      (request "/auth/csrf-token"))
            csrf (get-in sess [:response :body])
            response (-> sess
                          (request "/auth/login"
                                  :request-method :post
                                  :body (login-payload ident csrf)
                                  :content-type "application/json")
                          :response)]

        (is (= (:status response) 200))
        (is (= (current-user response) ident)))))

  (testing "failure"
    ;; stub out the callback to persona.
    (with-redefs [pk/verify-assertion (partial verify-assertion "failure")]
      (let [ident "foo@bar.org"
            sess (-> (session app)
                      (request "/")
                      (request "/auth/csrf-token"))
            csrf (get-in sess [:response :body])
            response (-> sess
                          (request "/auth/login"
                                  :request-method :post
                                  :body (login-payload ident csrf)
                                  :content-type "application/json")
                          :response)]
        (is (= (:status response) 403))))))

(deftest test-session-tokens
  (let [response (-> (session app)
                     (request "/")
                     (request "/auth/session")
                     :response)
        token    (str "Token: " (:body response))
        key-phrase "some very difficult key-phrase"
        principal (get-principal {:secrets {:key-phrase key-phrase}} token)
        not-key-phrase "different key phrase"]
    (testing "Getting a session token for an anonymous session"
      (is (= (:status response) 200))
      (is (not (nil? principal)))
      (is (not (= principal :staircase.handler/invalid)))
      (is (= principal
             (-> response :body jwt/str->jwt :claims :prn)))
      (is (= (get-principal {:secrets {:key-phrase not-key-phrase}}
                            token)
             :staircase.handler/invalid))))

  (testing "Session tokens refer to the same principal"
    (let [{r-1 :response :as sess} (-> (session app)
                                        (request "/")
                                        (request "/auth/session"))
          {r-2 :response} (request sess "/auth/session")]
      (is (= (as-prn r-1) (as-prn r-2)))))

  (testing "logging-in and getting a session token"
    ;; stub out the callback to persona.
    (with-redefs [pk/verify-assertion (partial verify-assertion "okay")]
      (let [ident "foo@bar.org"
            sess (-> (session app)
                      (request "/")
                      (request "/auth/csrf-token"))
            csrf (get-in sess [:response :body])
            principal (-> sess
                          (request "/auth/login"
                                  :request-method :post
                                  :body (login-payload ident csrf)
                                  :content-type "application/json")
                          (request "/auth/session")
                          :response
                          as-prn)]
        (is (= principal ident))))))

(deftest test-getting-histories
  (testing "Anonymous users can get histories"
    (let [{{token :body} :response :as s} (-> (session app)
                                              (request "/auth/session"))
          auth-header {"authorization" (str "Token: " token)}
          response (-> s
                        (request "/api/v1/histories"
                                :headers (assoc auth-header "accept" "application/json"))
                        :response)]
      (is (= 200 (:status response)))
      (is (not (nil? (:body response))))
      (is (= [] (json/parse-string (slurp (:body response))))))))

(deftest test-creating-histories
  (testing "Anonymous users can create histories"
    (let [{{token :body} :response :as s} (-> (session app)
                                              (request "/auth/session"))
          response (-> s
                       (ws-post "/api/v1/histories" token {:title "new history"})
                       :response)]
      (is (= 200 (:status response)))
      (is (not (nil? (:body response))))
      (when-let [body (:body response)]
        (let [history (-> body slurp json/parse-string)
              h-id    (history "id")]
          (is (not (nil? h-id)))
          (is (= "new history" (get history "title")))
          (is (not (nil? (get history "owner")))))))))

(deftest test-creating-steps
  (testing "Anonymous users can create steps"
    (let [{{token :body} :response :as s} (-> (session app)
                                              (request "/auth/session"))

          {{data :body} :response :as s} (ws-post s
                                              "/api/v1/histories"
                                              token
                                              {:title "new set of steps"})
          hid (some-> data slurp json/parse-string (get "id"))
          response (-> s
                       (ws-post
                         (str "/api/v1/histories/" hid "/steps")
                         token
                         {:title "foo"})
                       :response)]
      (is (not (nil? hid)))
      (is (= 200 (:status response)))
      (is (not (nil? (:body response))))
      (when-let [step (some-> response :body slurp json/parse-string)]
        (is (not (nil? (step "id"))))
        (is (= "foo" (get step "title")))
        (is (not (nil? (get step "owner"))))))))


