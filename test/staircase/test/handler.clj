(ns staircase.test.handler
  (:require [cheshire.core :as json]
            [staircase.data :as data]
            [com.stuartsierra.component :as component])
  (:use clojure.test
        staircase.protocols
        ring.mock.request  
        staircase.handler))

;; In-memory mock resource, holding a store of "things" keyed by their :id field.
(defrecord MockResource [things]
  Resource

  (exists? [this id] (not (nil? (get-one this id))))
  (get-all [_] things)
  (get-one [_ id] (first (filter #(= (str (:id %1)) (str id)) things)))
  (update [this id doc] (merge (get-one this id) doc))
  (delete [_ id])
  (create [_ doc] (doc "id")))

(defn json-request [meth path & [payload]]
  (let [req (-> (request meth path)
                (header "Accept" "application/json"))]
    (if payload
      (-> req (body (json/generate-string payload)) (content-type "application/json"))
      req)))

(defn data-is [resp expected]
  (is (= (json/parse-string (:body resp) true) expected)))

(deftest test-empty-app
  (let [histories (MockResource. [])
        steps (MockResource. [])
        app (-> (new-router) (assoc :histories histories :steps steps) (component/start) :handler)]

    (testing "GET /histories/1"
      (let [req (json-request :get "/histories/1")
            response (app req)]
        (is (= (:status response) 404))))

    (testing "PUT /histories/1"
      (let [response (app (json-request :put "/histories/1" {:updated true}))]
        (is (= (:status response) 404))))

    (testing "DELETE /histories/id"
      (let [response (app (request :delete "/histories/1"))]
        (is (= (:status response) 404))))

    (with-redefs [data/get-steps-of (constantly (get-all steps))]
      
      (testing "GET /histories/1/head"
        (let [req (json-request :get "/histories/1/head")
              response (app req)]
          (is (= (:status response) 404))))

      (testing "GET /histories/1/steps/1"
        (let [req (json-request :get "/histories/1/steps/1")
              response (app req)]
          (is (= (:status response) 404)))))

    (testing "GET /histories"
      (let [req (json-request :get "/histories")
            response (app req)]
        (is (= (:status response) 200))
        (data-is response [])))))


(deftest test-app-with-stuff
  (let [histories (MockResource. (map #(hash-map :id %1 :data "mock") (range 3)))
        steps     (MockResource. (map #(hash-map :id %1 :data "step") (range 5)))
        app (-> (new-router) (assoc :histories histories :steps steps) (component/start) :handler)]

    (testing "main route"
      (let [response (app (request :get "/"))]
        (is (= (:status response) 200))
        (is (= (:body response) "Hello World"))))

    (testing "GET /histories"
      (let [req (json-request :get "/histories")
            response (app req)]
        (is (= (:status response) 200))
        (data-is response [{:data "mock" :id 0} {:data "mock" :id 1} {:data "mock" :id 2}])))

    (testing "POST /histories"
      (let [req (json-request :post "/histories" {:id 1})
            response (app req)]
        (is (= (:status response) 200))
        (data-is response {:data "mock" :id 1})))

    (testing "GET /histories/1"
      (let [req (json-request :get "/histories/1")
            response (app req)]
        (is (= (:status response) 200))
        (data-is response {:data "mock" :id 1})))

    (with-redefs [data/get-steps-of (constantly (get-all steps))]
      
      (testing "GET /histories/1/head"
        (let [req (json-request :get "/histories/1/head")
              response (app req)]
          (is (= (:status response) 200))
          (data-is response {:data "step" :id 0})))

      (testing "GET /histories/1/steps/1"
        (let [req (json-request :get "/histories/1/steps/1")
              response (app req)]
          (is (= (:status response) 200))
          (data-is response {:data "step" :id 1}))))

    (testing "PUT /histories/1"
      (let [response (app (json-request :put "/histories/1" {:updated true}))]
        (is (= (:status response) 200))
        (data-is response {:data "mock" :id 1 :updated true})))

    (testing "DELETE /histories/id"
      (let [response (app (request :delete "/histories/1"))]
        (is (= (:status response) 204))))
    
    (testing "not-found route"
      (let [response (app (request :get "/invalid"))]
        (is (= (:status response) 404))))))
