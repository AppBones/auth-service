(ns auth-service.handlers
  (:require [clojure.string :refer [split]]
            [hiccup.page :refer [html5]]
            [hiccup.util :refer [escape-html]]
            [ring.util.response :as response])
  (:import (io.oauth.server.api AuthorizationApi ClientApi)))

(defn authorize-page [user app desc]
  (html5 {:lang "en"}
         [:body
          [:p (str "Hello, " (escape-html user) "!")]
          [:p (str (escape-html app) " wants access to your account on AppBone.")]
          [:p (escape-html desc)]
          [:form {:role "form" :method "POST"}
           [:input {:type "hidden" :name "decision" :value "1"}]
           [:input {:type "submit" :value "Authorize"}]]
          [:form {:role "form" :method "POST"}
           [:input {:type "hidden" :name "decision" :value "0"}]
           [:input {:type "submit" :value "Deny"}]]]))

(defn get-authorize [request db oauth]
  (let [client_id (get-in request [:query-params "client_id"])
        user "demo"]
    (if (nil? client_id)
      {:status 400 :body "No client_id provided."}
      (try
        (let [client (.getClient (:client oauth) client_id)
              client_name (.getName client)
              client_desc (.getDescription client)]
          (authorize-page user client_name client_desc))
        (catch io.oauth.server.ApiException e
          (prn e)
          {:status 404 :body "No client matches given client_id."})))))

(defn post-authorize [request db oauth]
  (let [client_id (get-in request [:params :client_id])
        decision (get-in request [:params :decision])
        user "demo"]
    (try
      (let [client (.getClient (:client oauth) client_id)
            re (.getRedirectUri client)
            rtype (get-in request [:params :response_type])
            state (get-in request [:params :state])
            scope (get-in request [:params :scope])
            cb (.authorize (:authorization oauth) client_id decision user nil re state rtype)
            uri (.getCallbackUri cb)]
        (response/redirect uri))
      (catch io.oauth.server.ApiException e
        (println "authem")
        (prn e)
        {:body (.getMessage e)}))))

(defn post-token [request db oauth]
  (let [{:keys [client_id client_secret grant_type scope code refresh_token]}
        (:params request)]
    (try
      (let [ts (-> (:authorization oauth)
                   (.token client_id client_secret grant_type scope code refresh_token))
            token (.toJson ts)]
        (response/content-type {:body token} "application/json"))
      (catch io.oauth.server.ApiException e
        (println "postem")
        (prn e)
        {:body (.getMessage e)}))))

(defn check-token [request db oauth]
  (let [token (or (get-in request [:params :access_token])
                  (last (split (get-in request [:headers "authorization"]) #" ")))]
    (try
      (let [tokeninfo (.check (:authorization oauth) token)
            info (.toJson tokeninfo)]
        (response/content-type {:body info} "application/json"))
      (catch io.oauth.server.ApiException e
        (println "checkem")
        (prn e)
        {:body (.getMessage e)}))))
