(ns staircase.test.assets
  (:import [java.io File])
  (:require [clj-rhino :as js]
            [clojure.java.io :as io])
  (:use clojure.test
        staircase.assets))

(def ls-file (io/as-file (io/resource "assets/test.ls")))
(def cs-file (io/as-file (io/resource "assets/test.coffee")))
(def less-file (io/as-file (io/resource "assets/test.less")))

(defn run-script [script]
  (let [scope (js/new-safe-scope)]
    (js/eval scope script)))

(defn boom! []
  (throw (Exception. "BOOM!")))

(def i-know-nothing! {:status 404 :body "beats me"})

(def fake-index {:status 200 :body "Welcome!"})

(def expected-css (-> "assets/expected.css"
                      io/resource 
                      slurp
                      (.trim)))

(defn GET [uri] {:request-method :get :uri uri})

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
                 :js-dir "/js"
                 :css-dir "/css"
                 :as-resource "assets"}]
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
      (let [f (asset-file-for {:uri "/css/test.css"} options)]
        (is f)
        (is (.equals less-file f)))
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
  (testing "coffee-script"
    (let [resp (generate-response cs-file)
          result (run-script (:body resp))]
      (is (= "OMG-CS: FOO!" result))))
  (testing "live-script"
    (let [resp (generate-response ls-file)
          result (run-script (:body resp))]
      (is (= "OMG-LS: FOO!" result))))
  (testing "less"
    (let [expected (-> "assets/expected.css"
                       io/resource 
                       slurp
                       (.trim))
          css      (-> less-file
                       generate-response
                       :body
                       (.trim))]
      (is (= expected css)))))

(deftest test-serve-asset
  (testing "no cache"
    (let [options {:no-cache? true}]
      (testing "coffee-script"
        (let [resp (serve-asset cs-file options)
              result (run-script (:body resp))]
          (is (= "OMG-CS: FOO!" result))
          (with-redefs [generate-response boom!]
            (is (thrown? Exception (serve-asset cs-file options))))))
      (testing "live-script"
        (let [resp (serve-asset ls-file options)
              result (run-script (:body resp))]
          (is (= "OMG-LS: FOO!" result)))
          (with-redefs [generate-response boom!]
            (is (thrown? Exception (serve-asset ls-file options)))))
      (testing "less"
        (let [expected (-> "assets/expected.css"
                          io/resource 
                          slurp
                          (.trim))
              css      (-> (serve-asset less-file options)
                          :body
                          (.trim))]
          (is (= expected css))))))
  (testing "with cache"
    (let [options {}]
      (testing "coffee-script"
        (let [resp (serve-asset cs-file options)
              result (run-script (:body resp))]
          (with-redefs [generate-response boom!]
            (is (= "OMG-CS: FOO!" result))
            (is (= resp (serve-asset cs-file options))))))
      (testing "live-script"
        (let [resp (serve-asset ls-file options)
              result (run-script (:body resp))]
          (with-redefs [generate-response boom!]
            (is (= "OMG-LS: FOO!" result))
            (is (= resp (serve-asset ls-file options))))))
      (testing "less"
        (let [compile-css #(-> % (serve-asset options) :body (.trim))
              css      (compile-css less-file)]
          (with-redefs [generate-response boom!]
            (is (= expected-css css))
            (is (= expected-css (compile-css less-file)))))))))

(deftest test-serve
  (let [options {:ls "ls" :coffee "coffee"
                 :js-dir "/js"
                 :css-dir "/css"
                 :as-resource "assets"}]
    (testing "non-asset requests"
      (are [req] (is (= nil (serve options req)))
          {:request-method :post :uri "/"}
          {:request-method :get  :uri "/foo"}
          {:request-method :get  :uri "/foo/bar.unknown"}))
    (testing "requests for non-existent assets"
      (are [req] (is (= nil (serve options req)))
          {:request-method :get :uri "/foo.js"}
          {:request-method :get :uri "/bar.coffee"}
          {:request-method :get :uri "/foo/bar.ls"}))
    (testing "requests for real assets"
      (let [resp (serve options
                        {:request-method :get :uri "/js/test.js"})]
        (is (= 200 (:status resp)))
        (is (= "OMG-CS: FOO!" (run-script (:body resp)))))
      (let [resp (serve options
                        {:request-method :get :uri "/js/test.coffee"})]
        (is (= 200 (:status resp)))
        (is (= "OMG-CS: FOO!" (run-script (:body resp)))))
      (let [resp (serve options
                        {:request-method :get :uri "/js/test.ls"})]
        (is (= 200 (:status resp)))
        (is (= "OMG-LS: FOO!" (run-script (:body resp)))))
      (let [resp (serve options
                        {:request-method :get :uri "/css/test.css"})]
        (is (= 200 (:status resp)))
        (is (= expected-css (-> resp :body (.trim)))))
      (let [resp (serve options
                        {:request-method :get :uri "/test.less"})]
        (is (= 200 (:status resp)))
        (is (= expected-css (-> resp :body (.trim))))))))

(deftest test-pipeline
  (testing "default pipeline"
    (let [pipeline (pipeline :ls "ls" :coffee "coffee"
                             :js-dir "/js"
                             :css-dir "/css"
                             :as-resource "assets")
          handler (fn [{uri :uri :as req}] (if (= uri "/") fake-index i-know-nothing!))
          app (pipeline handler)]
      (testing "non-asset requests"
        (is (= "Welcome!" (:body (app (GET "/")))))
        (are [req] (is (= "beats me" (:body (app req))))
             {:request-method :post :uri "/foo.js"}
             {:request-method :get  :uri "/foo"}
             {:request-method :get  :uri "/foo/bar.unknown"}))
      (testing "requests for non-existent assets"
        (are [req] (is (= "beats me" (:body (app req))))
            (GET "/foo.js")
            (GET "/bar.coffee")
            (GET "/bar.css")
            (GET "/quux.less")
            (GET "/foo/bar.ls")))
      (testing "requests for real assets"
            (let [resp (app (GET "/js/test.js"))]
              (is (= 200 (:status resp)))
              (is (= "OMG-CS: FOO!" (run-script (:body resp)))))
            (let [resp (app (GET "/js/test.coffee"))]
              (is (= 200 (:status resp)))
              (is (= "OMG-CS: FOO!" (run-script (:body resp)))))
            (let [resp (app (GET "/js/test.ls"))]
              (is (= 200 (:status resp)))
              (is (= "OMG-LS: FOO!" (run-script (:body resp)))))
            (let [resp (app (GET "/css/test.css"))]
              (is (= 200 (:status resp)))
              (is (= expected-css (-> resp :body (.trim)))))
            (let [resp (app (GET "/test.less"))]
              (is (= 200 (:status resp)))
              (is (= expected-css (-> resp :body (.trim))))))
      )))

