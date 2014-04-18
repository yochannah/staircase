(ns staircase.test.handler
  (:require [cheshire.core :as json]
            [staircase.data :as data]
            [ring.middleware.session.memory :refer (memory-store)]
            [com.stuartsierra.component :as component])
  (:use clojure.test
        staircase.protocols
        ring.mock.request  
        staircase.handler))

(defn asset-pipeline [f] f)

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

(defn get-router [histories steps]
  (-> (new-router)
      (assoc :asset-pipeline asset-pipeline
             :config {}
             :secrets {}
             :session-store (memory-store)
             :histories histories
             :steps steps)
      (component/start)
      :handler))

(deftest test-empty-app
  (let [histories (MockResource. [])
        steps (MockResource. [])
        app (get-router histories steps)]

    (testing "GET /api/v1/histories/1"
      (let [req (json-request :get "/api/v1/histories/1")
            response (app req)]
        (is (= (:status response) 404))))

    (testing "PUT /api/v1/histories/1"
      (let [response (app (json-request :put "/api/v1/histories/1" {:updated true}))]
        (is (= (:status response) 404))))

    (testing "DELETE /api/v1/histories/id"
      (let [response (app (request :delete "/api/v1/histories/1"))]
        (is (= (:status response) 404))))

    (with-redefs [data/get-steps-of (constantly (get-all steps))]
      
      (testing "GET /api/v1/histories/1/head"
        (let [req (json-request :get "/api/v1/histories/1/head")
              response (app req)]
          (is (= (:status response) 404))))

      (testing "GET /api/v1/histories/1/steps/1"
        (let [req (json-request :get "/api/v1/histories/1/steps/1")
              response (app req)]
          (is (= (:status response) 404)))))

    (testing "GET /api/v1/histories"
      (let [req (json-request :get "/api/v1/histories")
            response (app req)]
        (is (= (:status response) 200))
        (data-is response [])))

    (testing "GET /api/v1/steps"
      (let [req (json-request :get "/api/v1/steps")
            response (app req)]
        (is (= (:status response) 200))
        (data-is response [])))

    (testing "GET /api/v1/steps/1"
      (let [req (json-request :get "/api/v1/steps/1")
            response (app req)]
        (is (= (:status response) 404))))))

(deftest test-app-with-stuff
  (let [histories (MockResource. (map #(hash-map :id %1 :data "mock") (range 3)))
        steps     (MockResource. (map #(hash-map :id %1 :data "step") (range 5)))
        app (get-router histories steps)]

    (testing "main route"
      (let [response (app (request :get "/"))]
        (is (= (:status response) 200))
        (is (= (get-in response [:headers "Content-Type"]) "text/html; charset=utf-8"))))

    (testing "GET /histories"
      (let [req (json-request :get "/api/v1/histories")
            response (app req)]
        (is (= (:status response) 200))
        (data-is response [{:data "mock" :id 0} {:data "mock" :id 1} {:data "mock" :id 2}])))

    (testing "POST /histories"
      (let [req (json-request :post "/api/v1/histories" {:id 1})
            response (app req)]
        (is (= (:status response) 200))
        (data-is response {:data "mock" :id 1})))

    (testing "GET /histories/1"
      (let [req (json-request :get "/api/v1/histories/1")
            response (app req)]
        (is (= (:status response) 200))
        (data-is response {:data "mock" :id 1})))

    (testing "PUT /histories/1"
      (let [response (app (json-request :put "/api/v1/histories/1" {:updated true}))]
        (is (= (:status response) 200))
        (data-is response {:data "mock" :id 1 :updated true})))

    (testing "DELETE /histories/id"
      (let [response (app (request :delete "/api/v1/histories/1"))]
        (is (= (:status response) 204))))

    (with-redefs [data/get-steps-of (constantly (get-all steps))]
      
      (testing "GET /histories/1/head"
        (let [req (json-request :get "/api/v1/histories/1/head")
              response (app req)]
          (is (= (:status response) 200))
          (data-is response {:data "step" :id 0})))

      (testing "GET /histories/1/steps"
        (let [req (json-request :get "/api/v1/histories/1/steps")
              response (app req)]
          (is (= (:status response) 200))
          (data-is response [{:data "step" :id 0}
                             {:data "step" :id 1}
                             {:data "step" :id 2}
                             {:data "step" :id 3}
                             {:data "step" :id 4}])))

      (testing "GET /api/v1/histories/1/steps/1"
        (let [req (json-request :get "/api/v1/histories/1/steps/1")
              response (app req)]
          (is (= (:status response) 200))
          (data-is response {:data "step" :id 1}))))
      
    (testing "POST /api/v1/histories/1/steps"
      (let [req (json-request :post "/api/v1/histories/1/steps" {:new "step" :id 4})
            response (app req)]
        (is (= (:status response) 200))
        (data-is response {:data "step" :id 4})))

    (testing "GET /api/v1/steps"
      (let [req (json-request :get "/api/v1/steps")
            response (app req)]
        (is (= (:status response) 200))
        (data-is response [{:data "step" :id 0}
                            {:data "step" :id 1}
                            {:data "step" :id 2}
                            {:data "step" :id 3}
                            {:data "step" :id 4}])))

    (testing "GET /steps/2 OK"
      (let [req (json-request :get "/api/v1/steps/2")
            response (app req)]
        (is (= (:status response) 200))
        (data-is response {:data "step" :id 2})))

    (testing "GET /steps/100 NOT FOUND"
      (let [req (json-request :get "/api/v1/steps/100")
            response (app req)]
        (is (= (:status response) 404))))

    (testing "DELETE /steps/2 OK"
      (let [req (json-request :delete "/api/v1/steps/2")
            response (app req)]
        (is (= (:status response) 204))))

    (testing "DELETE /steps/200 NOT FOUND"
      (let [req (json-request :delete "/api/v1/steps/200")
            response (app req)]
        (is (= (:status response) 404))))
    
    (testing "not-found route"
      (let [response (app (request :get "/invalid"))]
        (is (= (:status response) 404))))))
