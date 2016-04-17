(ns auth-service.core
  (:require [com.stuartsierra.component :as component]
            [environ.core :refer [env]]
            [auth-service.http :as http]
            [auth-service.routes :as routes]
            [auth-service.oauth :as oauth]
            [auth-service.db :as db])
  (:gen-class))

(defn opt-or-fallback
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
  (let [{:keys [domain http-port db-connection is-dev oauth-base-path
                oauthio-id oauthio-secret]} config-opts
        is-dev (if (nil? is-dev) (= "true" (env :is-dev)) is-dev)
        scheme (if is-dev "http" "https")
        domain (opt-or-fallback domain :domain (str "localhost:" http-port))
        http-port (read-string (opt-or-fallback http-port :port "8081"))
        basePath (opt-or-fallback oauth-base-path :oauth-base-path "oauth2")
        providerId (opt-or-fallback oauthio-id :oauthio-id "")
        providerSecret (opt-or-fallback oauthio-secret :oauthio-secret "")
        db-connection (opt-or-fallback db-connection :db-connection "")]
    (component/system-map
     :config-opts config-opts
     :db (db/map->DB {:conn db-connection})
     :oauth (oauth/map->OAuthProvider {:scheme scheme
                                       :domain domain
                                       :basePath basePath
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
