(ns staircase.test.api
  (:require [cheshire.core :as json]
            [persona-kit.core :as pk]
            [staircase.data :as data]
            [staircase.resources.histories :as hs]
            [com.stuartsierra.component :as component])
  (:use clojure.test
        staircase.test.util
        staircase.protocols
        ring.mock.request  
        staircase.handler)
  (import staircase.test.util.MockResource))

(defn data-is [resp expected]
  (is (= (json/parse-string (slurp (:body resp)) true) expected)))

(def mock-get-principal (constantly "foo@bar.org"))

(deftest test-empty-app
  (let [histories (MockResource. [])
        steps (MockResource. [])
        projects (MockResource. [])
        app (get-router {:histories histories :steps steps :projects projects})]

    (with-redefs [staircase.handler/get-principal mock-get-principal]

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

      (testing "GET /api/v1/projects"
        (let [req (json-request :get "/api/v1/projects")
              response (app req)]
          (is (= (:status response) 200))
          (data-is response [])))

      (testing "GET /api/v1/projects/1"
        (let [req (json-request :get "/api/v1/projects/1")
              response (app req)]
          (is (= (:status response) 404))))

      (testing "DELETE /api/v1/projects/1"
        (let [req (json-request :delete "/api/v1/projects/1")
              response (app req)]
          (is (= (:status response) 404))))

      (with-redefs [hs/get-steps-of (constantly (get-all steps))]
        
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
          (is (= (:status response) 404)))))))

(deftest test-app-with-stuff
  (let [histories (MockResource. (map #(hash-map :id %1 :data "mock") (range 3)))
        steps     (MockResource. (map #(hash-map :id %1 :data "step") (range 5)))
        app (get-router {:histories histories :steps steps})]

    (with-redefs [staircase.handler/get-principal mock-get-principal]

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

      (with-redefs [hs/get-steps-of (constantly (get-all steps))]
        
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

      (testing "POST /api/v1/histories/1/steps/2/fork"
        (let [added (atom nil)]
          (with-redefs [hs/get-history-steps-of (constantly [:a :b])
                        hs/add-all-steps (fn [hs id steps] (swap! added (constantly steps)))]
            (let [req (json-request :post "/api/v1/histories/1/steps/2/fork" {})
                  response (app req)]
              (is (= (:status response) 200))
              (is (= @added [:a :b]))))))

      (testing "POST /api/v1/histories/10/steps/2/fork"
        (let [added (atom nil)]
          (with-redefs [hs/get-history-steps-of (constantly [:a :b])
                        hs/add-all-steps (fn [hs id steps] (swap! added (constantly steps)))]
            (let [req (json-request :post "/api/v1/histories/10/steps/2/fork" {})
                  response (app req)]
              (is (= (:status response) 404))
              (is (= @added nil))))))
        
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
          (is (= (:status response) 404)))))))
