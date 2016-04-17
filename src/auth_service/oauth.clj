(ns auth-service.oauth
  (:require [com.stuartsierra.component :as component])
  (:import (io.oauth.server.api AuthorizationApi ClientApi)))

(defrecord OAuthProvider [scheme domain basePath providerId providerSecret]
  component/Lifecycle

  (start [this]
    (println "Creating oauthd component ...")
    (AuthorizationApi/initialize providerId providerSecret)
    (let [path (str scheme "://" domain "/" basePath)
          auth (-> (AuthorizationApi.)
                   (doto (.setBasePath path)))
          client (-> (ClientApi.)
                     (doto (.setBasePath path)))]
      (-> this
          (assoc :authorization auth)
          (assoc :client client)
          (assoc :basePath basePath))))

  (stop [this]
    (assoc this :counter nil)))

