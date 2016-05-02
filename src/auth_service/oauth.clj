(ns auth-service.oauth
  (:require [com.stuartsierra.component :as component])
  (:import (io.oauth.server.api AuthorizationApi ClientApi)))

(defrecord OAuthProvider [basePath providerId providerSecret]
  component/Lifecycle

  (start [this]
    (println "Creating oauthd component ...")
    (let [authen (-> (AuthorizationApi.)
                     (doto (.setBasePath basePath)))
          client (-> (ClientApi.)
                     (doto (.setBasePath basePath)))]
      (AuthorizationApi/initialize providerId providerSecret)
      (-> this
          (assoc :authorization authen)
          (assoc :client client)
          (assoc :basePath basePath))))

  (stop [this]
    (-> this
        (assoc :authorization nil)
        (assoc :client nil)
        (assoc :basePath nil))))

