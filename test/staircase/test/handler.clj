(ns staircase.test.handler
  (:require [staircase.protocols])
  (:import staircase.protocols.Resource)
  (:use clojure.test
        ring.mock.request  
        staircase.handler))

(defrecord MockResource [existance things updated]
  staircase.protocols.Resource

  (exists? [_ id] existance)
  (get-all [_] things)
  (get-one [_ id] (first (filter #(= (:id %1) id) things)))
  (update [_ id doc] updated)
  (delete [_ id])
  (create [_ doc] (doc "id")))

(def histories (MockResource. true (map #(hash-map :id %1 :data "mock") (range 3)) {:updated true}))
(def steps (MockResource. true [] nil))

(def app (-> (new-router) (assoc :histories histories) (assoc :steps steps) (.start) :handler))

(defn json-request [meth path]
  (-> (request meth path) (header "Accept" "application/json") (content-type "application/json")))

(deftest test-app
  (testing "main route"
    (let [response (app (request :get "/"))]
      (is (= (:status response) 200))
      (is (= (:body response) "Hello World"))))

  (testing "GET /histories"
    (let [req (json-request :get "/histories")
          response (app req)]
      (is (= (:status response) 200))
      (is (= (:body response) "[{\"data\":\"mock\",\"id\":0},{\"data\":\"mock\",\"id\":1},{\"data\":\"mock\",\"id\":2}]"))))

  (testing "POST /histories"
    (let [req (-> (json-request :post "/histories")
                  (body "{\"id\": 1}"))
          response (app req)]
      (is (= (:status response) 200))
      (is (= (:body response) "{\"data\":\"mock\",\"id\":1}"))))
  
  (testing "not-found route"
    (let [response (app (request :get "/invalid"))]
      (is (= (:status response) 404)))))
