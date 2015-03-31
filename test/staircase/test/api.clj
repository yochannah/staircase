(ns staircase.test.api
  (:require [clojure.tools.logging :refer (info)]
            [cheshire.core :as json]
            [persona-kit.core :as pk]
            [staircase.data :as data]
            [staircase.resources.histories :as hs]
            [com.stuartsierra.component :as component])
  (:use clojure.test
        staircase.test.util
        staircase.protocols
        ring.mock.request  
        staircase.handler)
  (import [staircase.test.util MockStatefulResource MockResource]))

(defn data-is [resp expected & [msg]]
  (let [body (:body resp)
        data (try
               (json/parse-string (slurp body) true)
               (catch java.io.FileNotFoundException e ::ENODATA))]
    (is body "The response has a body")
    (is (= expected data) msg)))

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

(defn mock-projects []
  (let [mock-folder (fn [user id]
                      {"title" (str "project " id)
                       "id" id
                       "owner" user
                       :contents [{:id (* id id)}] ;; Fake nesting, for the links
                       :child_nodes [{:id (inc id)}] ;; More fake nesting
                       :parent_id (when (< 0 id) (dec id)) ;; fake nesting, purely for links.
                       :children []})
        user-mock-folders (->> (range 0 3)
                               (map (partial mock-folder (mock-get-principal))))
        other-mock-folders (->> (range 3 6)
                                (map (partial mock-folder "someone@else.org")))]
    (atomic-resource (concat user-mock-folders other-mock-folders))))

(deftest project-routes-get
  (let [projects (mock-projects)
        app (get-router {:projects projects})]
    (with-redefs [staircase.handler/get-principal mock-get-principal]

      (testing "GET /api/v1/projects"
        (let [req (json-request :get "/api/v1/projects")
              response (app req)]
          (is (= (:status response) 200))
          (data-is response [{:title "project 0",
                              :id 0,
                              :owner "foo@bar.org",
                              :parent_id nil
                              :contents [{:id 0}]
                              :child_nodes [{:id 1}]
                              :children []}
                             {:title "project 1",
                              :id 1,
                              :owner "foo@bar.org",
                              :parent_id 0
                              :contents [{:id 1}]
                              :child_nodes [{:id 2}]
                              :children []}
                             {:title "project 2",
                              :id 2,
                              :contents [{:id 4}]
                              :child_nodes [{:id 3}]
                              :owner "foo@bar.org",
                              :parent_id 1
                              :children []}])))

      (testing "GET /api/v1/projects/1"
        (let [req (json-request :get "/api/v1/projects/1")
              response (app req)]
          (is (= (:status response) 200))
          (data-is response {:title "project 1",
                             :id 1,
                             :contents [{:id 1}]
                             :child_nodes [{:id 2}]
                             :owner "foo@bar.org",
                             :parent_id 0
                             :children []}))))))

(deftest add-project

  (let [projects (mock-projects)
        app (get-router {:projects projects})]
    (with-redefs [staircase.handler/get-principal mock-get-principal]

      (testing "POST /api/v1/projects"
        (let [req (json-request :post "/api/v1/projects" {:title "added project"})
              resp (app req)]
          (is (= 200 (:status resp)))
          (data-is resp {:title "added project" :id 7 :owner "foo@bar.org"}))))))

(deftest add-item
  (let [location "/api/v1/projects/1/items/7"
        projects (mock-projects)
        app (get-router {:projects projects})]
    (with-redefs [staircase.handler/get-principal mock-get-principal]

      ;; The contract is that we can add items if the project exists. So
      ;; We first check the project exists.
      (testing "GET /api/v1/projects/1"
        (let [req (json-request :get "/api/v1/projects/1")
              resp (app req)]
          (is (= 200 (:status resp)) "The project should exist")))

      ;; Then we add the item, and fetch it by its URI.
      (testing "POST /api/v1/projects/1/items"
        (let [req (json-request :post "/api/v1/projects/1/items" {:item_name "I am an item"})
              resp (app req)]
          (is (= 204 (:status resp))
              "The request was successful, but there is no body")
          (is (= location (get-in resp [:headers "Location"]))
              "The response tells us where the representation is"))
        (let [req (json-request :get location)
              resp (app req)]
          (is (= 200 (:status resp))
              "The item exists")
          (is (= ["</api/v1/projects/1>; rel=\"project\""] (get-in resp [:headers "Link"]))
              "We have a HATEOS link")
          (data-is resp {:id 7 :item_name "I am an item"}
              "The item has the correct data"))))))

(deftest ^:api get-nested-project
  (let [projects (mock-projects)
        app (get-router {:projects projects})]
    (with-redefs [staircase.handler/get-principal mock-get-principal]

      (testing "GET /api/v1/projects/2"
        (let [req (json-request :get "/api/v1/projects/2")
              resp (app req)]
          (is (= 200 (:status resp)) "The project should exist")
          (is (= ["</api/v1/projects/1>; rel=\"project\""
                  "</api/v1/projects/2/items/4>; rel=\"item\""
                  "</api/v1/projects/3>; rel=\"subproject\""]
                 (get-in resp [:headers "Link"]))
              "We have HATEOS links back to the parent project, the items and the child nodes"))))))

(deftest ^:api delete-my-stuff

  (let [projects (mock-projects)
        app (get-router {:projects projects})]
    (with-redefs [staircase.handler/get-principal mock-get-principal]
      (testing "DELETE /api/v1/projects/2"
        (let [del-req (json-request :delete "/api/v1/projects/2")
              get-req (json-request :get "/api/v1/projects/2")
              resp (app del-req)
              resp-2 (app get-req)]
          (is (= 204 (:status resp)) "The request should be accepted")
          (is (= 404 (:status resp-2)) "The resource should now be gone"))))))

(deftest ^:api delete-your-stuff

  (let [projects (mock-projects)
        app (get-router {:projects projects})]
    (with-redefs [staircase.handler/get-principal mock-get-principal]
      (testing "DELETE /api/v1/projects/4"
        (let [del-req (json-request :delete "/api/v1/projects/4")
              resp (app del-req)]
          (is (= 404 (:status resp)) "The request should be refused with 404"))))))

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
