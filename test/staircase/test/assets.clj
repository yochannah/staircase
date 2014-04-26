(ns staircase.test.assets
  (:import [java.io File])
  (:require [clj-rhino :as js]
            [clojure.java.io :as io])
  (:use clojure.test
        staircase.assets))

(def ls-file (io/as-file (io/resource "assets/test.ls")))
(def cs-file (io/as-file (io/resource "assets/test.coffee")))

(defn run-script [script]
  (let [scope (js/new-safe-scope)]
    (js/eval scope script)))

(deftest test-ext
  (testing "nil input"
    (is (= nil (ext nil))))
  (testing "simple extension"
    (is (= :txt (ext (File. "text.txt"))))
    (is (= :js (ext (File. "foo.js")))))
  (testing "with a path"
    (is (= :js (ext (File. "some/file/with/path.js"))))))

(deftest test-file-md5
  (let [cksm (checksum ls-file)]
    (testing "checksum of a file exists"
      (is cksm))
    (testing "checksum is stable"
      (is (= cksm (checksum ls-file))))
    (testing "checksum differs between files"
      (is (not (= cksm (checksum cs-file)))))
    (testing "checksumming non-existent files"
      (is (thrown? Exception (checksum (File. "foo.bar")))))))

(deftest test-get-kind
  (are [file-name kind] (= (get-kind file-name) kind)
       "foo.js" :script
       "foo.coffee" :coffee
       "foo.ls" :ls
       "foo.less" :less
       "foo.css" :style))

(deftest test-script-candidates
  (let [options {:ls "ls" :coffee "coffee" :js-dir "js" :as-resource "res"}]
    (testing "candidates for js file"
      (let [candidates (script-candidates "js/foo.js" options)]
        (is (= 4 (count candidates)))
        (is (every? #{:coffee :ls} (map ext (filter identity candidates))))
        ))
    (testing "candidates for coffee file"
      (let [candidates (script-candidates "foo/bar.coffee" options)]
        (is (= 4 (count candidates)))
        (is (= #{:coffee} (set (map ext (filter identity candidates)))))
        ))))

(deftest test-candidates-for
  (let [options {:ls "ls" :coffee "coffee" :js-dir "js" :as-resource "res"}]
    (testing "candidates for js file"
      (let [candidates (candidates-for {:uri "/js/foo.js"} options)]
        (is (= 4 (count candidates)))
        (is (every? #{:coffee :ls} (map ext (filter identity candidates))))
        ))
    (testing "candidates for coffee file"
      (let [candidates (candidates-for {:uri "/foo/bar.coffee"} options)]
        (is (= 4 (count candidates)))
        (is (= #{:coffee} (set (map ext (filter identity candidates)))))
        ))))

(deftest test-asset-file-for
  (let [options {:ls "ls" :coffee "coffee"
                 :js-dir "js" :as-resource "assets"}]
    (testing "asset file for non-existent file"
      (is (= nil (asset-file-for {:uri "/js/foo.js"} options))))
    (testing "asset file for existing file - real path"
      (let [f (asset-file-for {:uri "/test.coffee"} options)]
        (is f)
        (is (.equals cs-file f)))
      (let [f (asset-file-for {:uri "/test.ls"} options)]
        (is f)
        (is (.equals ls-file f))))
    (testing "asset file for existing file - transformed path"
      (let [f (asset-file-for {:uri "/js/test.js"} options)]
        (is f) ;; Prefers coffee-script files...
        (is (.equals cs-file f)))
      (let [options (assoc options :exts [:ls :coffee])
            f (asset-file-for {:uri "/js/test.js"} options)]
        (is f) ;; But can be configured to look for any extension
        (is (.equals ls-file f))))))

(deftest test-is-asset-req
  (are [req ok] (is (= ok (is-asset-req req)))
       {:request-method :get :uri "/foo.js"} true
       {:request-method :get :uri "/foo.coffee"} true
       {:request-method :get :uri "/foo.ls"} true
       {:request-method :get :uri "/foo.less"} true
       {:request-method :get :uri "/foo.css"} true
       {:request-method :get :uri "/foo"} false
       {:request-method :post :uri "/foo.js"} false
       {:request-method :post :uri "/foo.coffee"} false
       {:request-method :post :uri "/foo.ls"} false
       {:request-method :post :uri "/foo.less"} false
       {:request-method :post :uri "/foo.css"} false))

(deftest test-generate-response
  (let [resp (generate-response cs-file)
        result (run-script (:body resp))]
    (is (= "OMG-CS: FOO!" result)))
  (let [resp (generate-response ls-file)
        result (run-script (:body resp))]
    (is (= "OMG-LS: FOO!" result))))

