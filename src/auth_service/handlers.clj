(ns auth-service.handlers
  (:require [hiccup.page :refer [html5]]
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
           [:input {:type "hidden" :name "Decision" :value "1"}]
           [:input {:type "submit" :value "Authorize"}]]
          [:form {:role "form" :method "POST"}
           [:input {:type "hidden" :name "Decision" :value "0"}]
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
  (let [client_id (get-in request [:query-params "client_id"])
        decision (get-in request [:form-params "decision"])
        user "demo"]
    (try
      (let [client (.getClient (:client oauth) client_id)
            re (.getRedirectUri client)
            rtype (get-in request [:query-params "response_type"])
            state (get-in request [:query-params "state"])
            scope (get-in request [:query-params "scope"])
            cb (.authorize (:authorization oauth) client_id decision user scope re state rtype)
            uri (.getCallbackUri cb)]
        (response/redirect uri))
      (catch io.oauth.server.ApiException e
        (prn e)
        {:status 400 :body (.toString e)}))))

(defn get-token [request db oauth]
  "I don't do much yet")
