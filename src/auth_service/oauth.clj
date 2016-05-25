(ns auth-service.oauth
  (:require [clojure.walk :refer [keywordize-keys]]
            [clojure.data.codec.base64 :as b64]
            [clojure.data.json :as json]
            [com.stuartsierra.component :as component]
            [ring.util.response :as resp]
            [buddy.sign.jwe :as jwe]
            [buddy.sign.jws :as jws]
            [buddy.core.keys :as keys]
            [org.httpkit.client :as http]))

(defrecord OAuthProvider [basePath providerId providerSecret jwt-key key-pass]
  component/Lifecycle

  (start [this]
    (println "Creating oauthd component ...")
    (-> this
        (assoc :oauthd-id providerId)
        (assoc :oauthd-secret providerSecret)
        (assoc :oauthd-path basePath)
        (assoc :jwt-key (keys/str->private-key jwt-key key-pass))))

  (stop [this]
    (-> this
        (assoc :oauthd-id nil)
        (assoc :oauthd-secret nil)
        (assoc :jwt-key nil)
        (assoc :oauthd-path nil))))

(defn authp [request cfg]
  (let [token (str (:oauthd-id cfg) ":" (:oauthd-secret cfg))
        header (String. (b64/encode (.getBytes token "UTF-8")) "UTF-8")]
    (assoc-in request [:headers "Authorizationp"] (str "Basic " header))))

(defn wrap-authp [handler cfg]
  "Ring middleware for adding the Authorizationp header to a request, for
  authenticating with an oauthd instance."
  (fn [request]
    (authp request cfg)
    (handler request)))

(defn get-client [cfg client_id]
  "Fetch the oauthd data for client matching client_id."
  (let [options (authp {:accept "application/json"} cfg)
        url (str (:oauthd-path cfg) "/clients/" client_id)
        {:keys [status body error opts]} @(http/get url options)]
    (if (or error (not= status 200))
      nil
      (:data (keywordize-keys (json/read-str body))))))

(defn authorize [handler cfg user_id]
  "Given a ring-request and the id of an authenticated user, forward the
  appropriate parameters to the configured oauthd instance and construct the
  ring-response expected by oauthd's oauth2 flow on success."
  (fn [request]
    (let [options (-> request
                      (select-keys [:query-params :form-params])
                      (assoc-in [:query-params "user_id"] user_id)
                      (authp cfg))
          url (str (:oauthd-path cfg) "/authorization")
          {:keys [status body error opts]} @(http/post url options)]
      (if error
        (-> (resp/response "Internal Server Error")
            (resp/status 500))
        (if (= status 200)
          (-> (handler request)
              (assoc :status 302)
              (assoc-in [:headers "Location"] (:url opts)))
          (-> (handler request)
              (assoc :status status)
              (assoc :body body)))))))

(defn token [handler cfg]
  "Wrap a given ring-request for a token and forward it to the configured oauthd
  instance, returning a token set if successful."
  (fn [request]
    (let [options (-> request
                      (select-keys [:query-params :form-params])
                      (authp cfg))
          url (str (:oauthd-path cfg) "/token")
          {:keys [status body error opts]} @(http/post url options)]
      (if error
        (-> (resp/response "Internal Server Error")
            (resp/status 500))
        (-> (handler request)
            (assoc-in [:headers "Content-Type"] "application/json")
            (assoc :status status)
            (assoc :body body))))))

(defn check [handler cfg]
  "Wrap a given ring-request for a token and forward it to the configured oauthd
  instance, returning a token set if successful."
  (fn [request]
    (let [options (-> request
                      (select-keys [:query-params])
                      (authp cfg))
          url (str (:oauthd-path cfg) "/check")
          {:keys [status body error opts]} @(http/get url options)]
      (if error
        (-> (resp/response "Internal Server Error")
            (resp/status 500))
        (-> (handler request)
            (assoc-in [:headers "Content-Type"] "application/json")
            (assoc :status status)
            (assoc :body body))))))

(defn ac->jwt [handler cfg]
  "Wrap a handler with middleware to obtain a check of an access token, then
  encode the response as a JWT enriched with the apropos user's data."
  (fn [request]
    (let [h (check handler cfg)
          check (h request)]
      (if (= (:status check) 200)
        (let [token (json/read-str (:body check))
              jwt (jwe/encrypt token (:jwt-key cfg) {:alg :rsa-oaep-256
                                                     :enc :a128cbc-hs256})]
          (resp/response jwt))
        check))))
