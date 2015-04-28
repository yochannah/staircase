(ns staircase.tokens
  (:require [clj-jwt.core  :refer :all]
            [clj-jwt.key   :refer [private-key public-key]]
            [clj-jwt.intdate :refer [intdate->joda-time]]
            [clj-time.core :refer (now plus days after?)]))

;; Construct a jwt-token for the given identity.
(defn issue-session [config secrets ident]
  (let [key-phrase (:key-phrase secrets)
        claim {:iss (:audience config)
               :exp (plus (now) (days 1))
               :iat (now)
               :prn ident}]
    (if-let [rsa-prv-key
             (try (private-key "rsa/private.key" key-phrase)
               (catch java.io.FileNotFoundException fnf nil))]
      (-> claim jwt (sign :RS256 rsa-prv-key) to-str)
      (-> claim jwt (sign :HS256 key-phrase) to-str))))

;; Get claims if valid, else nil
(defn valid-claims [secrets token]
  (let [web-token (str->jwt token)
        claims (:claims web-token)
        proof (try (public-key  "rsa/public.key")
                   (catch java.io.FileNotFoundException fnf
                     (:key-phrase secrets)))]
    (when (and (verify web-token proof)
               (after? (intdate->joda-time (:exp claims)) (now)))
      claims)))
