(ns auth-service.handlers
  (:require [clojure.string :refer [split]]
            [hiccup.page :refer [html5]]
            [hiccup.util :refer [escape-html]]
            [ring.util.response :as response]
            [auth-service.oauth :as oauth]))

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
  ;; TODO: look-up user-id based on auth data
  (let [client_id (get-in request [:query-params "client_id"])
        user "demo"]
    (if (nil? client_id)
      {:status 400 :body "No client_id provided."}
      (let [{:keys [name description]} (oauth/get-client oauth client_id)]
        (authorize-page user name description)))))

(defn post-authorize [request db oauth]
  ;; TODO: look-up user-id based on auth data
  (oauth/authorize request oauth "demo"))
