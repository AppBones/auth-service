(ns auth-service.core
  (:require [com.stuartsierra.component :as component]
            [environ.core :refer [env]]
            [auth-service.http :as http]
            [auth-service.routes :as routes]
            [auth-service.oauth :as oauth]
            [auth-service.db :as db])
  (:gen-class))

(defn get-opt
  "Performs a series of passthroughs in the event that the given configuration
  option does not exist.
  First, it looks up env-var in the environment, and if that is also nil, it returns the provided default."
  [opt env-var default]
  (if (nil? opt)
    (if (nil? (env env-var))
      default
      (env env-var))
    opt))

(defn create-service [config-opts]
  "wires up the web service's dependency graph with provided configuration"
  (let [{:keys [domain http-port db-connection is-dev oauthd-url
                oauthd-id oauthd-secret]} config-opts
        is-dev (if (nil? is-dev) (= "true" (env :is-dev)) is-dev)
        http-port (read-string (get-opt http-port :port "8081"))
        oauthd-url (get-opt oauthd-url :oauthd-url "https://oauth.io/oauth2")
        providerId (get-opt oauthd-id :oauthd-id "")
        providerSecret (get-opt oauthd-secret :oauthd-secret "")
        db-connection (get-opt db-connection :db-connection "")]
    (component/system-map
     :config-opts config-opts
     :db (db/map->DB {:conn db-connection})
     :oauth (oauth/map->OAuthProvider {:basePath oauthd-url
                                       :providerId providerId
                                       :providerSecret providerSecret})
     :routes (component/using
              (routes/map->Routes {})
              [:db :oauth])
     :http (component/using
            (http/map->HTTP {:port http-port})
            [:routes]))))

(defn -main [& args]
  "entry point for executing outside of a REPL"
  (let [system (create-service {:is-dev false})]
    (component/start system)))
