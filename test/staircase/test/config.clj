(ns staircase.test.config
  (:use clojure.test
        staircase.config))

(deftest test-empty-env
  (let [opts {}]
    (testing "Empty options should produce empty subsets."
      (is (= {} (db-options opts))))))

(deftest test-one-relevant-option
  (let [opts (db-options {:db-opt "some option" :other-opt "not a db option"})]
    (testing "Only collects the options we want"
      (is (= 1 (count opts))))
    (testing "Strips the prefix"
      (is (= [:opt] (keys opts))))
    (testing "Collects the right values"
      (is (= {:opt "some option"} opts)))))

(deftest a-mixed-bag
  (let [opts (db-options {:db-opt-1 "some option" :db-opt-2 "another opt" :other-opt "not a db option"})]
    (testing "Only collects the options we want"
      (is (= 2 (count opts))))
    (testing "Collects the right values"
      (is (= {:opt-1 "some option" :opt-2 "another opt"} opts)))))

