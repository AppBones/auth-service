(ns auth-service.oauth
  (:require [clojure.walk :refer [keywordize-keys]]
            [clojure.data.codec.base64 :as b64]
            [clojure.data.json :as json]
            [com.stuartsierra.component :as component]
            [ring.util.response :as resp]
            [org.httpkit.client :as http]))

(defrecord OAuthProvider [basePath providerId providerSecret]
  component/Lifecycle

  (start [this]
    (println "Creating oauthd component ...")
    (-> this
        (assoc :oauthd-id providerId)
        (assoc :oauthd-secret providerSecret)
        (assoc :oauthd-path basePath)))

  (stop [this]
    (-> this
        (assoc :oauthd-id nil)
        (assoc :oauthd-secret nil)
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
          {:keys [status body error opts]} @(http/post url options)]
      (if error
        (-> (resp/response "Internal Server Error")
            (resp/status 500))
        (-> (handler request)
            (assoc-in [:headers "Content-Type"] "application/json")
            (assoc :status status)
            (assoc :body body))))))

