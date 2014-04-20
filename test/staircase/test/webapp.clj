(ns staircase.test.webapp
  (:require [cheshire.core :as json]
            [persona-kit.core :as pk]
            [ring.middleware.anti-forgery :as af]
            [staircase.data :as data]
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

(defn payload
  [ident csrf]
  (-> (json/generate-string {:assertion ident
                             :__anti-forgery-token csrf})
      (.getBytes "UTF-8")
      clojure.java.io/input-stream))

(defn current-user
  [response]
  (-> response :body slurp json/parse-string (get "current")))

(deftest test-webapp
  (let [histories (MockResource. (map #(hash-map :id %1 :data "mock") (range 3)))
        steps     (MockResource. (map #(hash-map :id %1 :data "step") (range 5)))
        app (get-router histories steps)]

    (testing "main route"
      (let [{:keys [response]} (-> (session app)
                         (request "/"))
            status (:status response)
            content-type (get-in response [:headers "Content-Type"])]
        (is (= status 200))
        (is (= content-type "text/html; charset=utf-8"))))

    (testing "csrf tokens are stable within a session"
      (let [sess (-> (session app)
                     (request "/auth/csrf-token"))
            csrf (get-in sess [:response :body])
            sess (request sess "/auth/csrf-token")
            csrf' (get-in sess [:response :body])]
        (is (and csrf csrf'))
        (is (= csrf csrf'))))

    (testing "logging-in - success"
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
                                    :body (payload ident csrf)
                                    :content-type "application/json")
                           :response)]

          (is (= (:status response) 200))
          (is (= (current-user response) ident)))))

    (testing "logging-in - failure"
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
                                    :body (payload ident csrf)
                                    :content-type "application/json")
                           :response)]
          (is (= (:status response) 403)))))

    ))

