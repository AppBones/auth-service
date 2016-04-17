(ns auth-service.http
  (:require [compojure.handler :as compojure-handler]
            [com.stuartsierra.component :as component]
            [org.httpkit.server :refer [run-server]]))

;; Create Server component to run http-kit server,
;; using ring handlers created in Routes component
(defn get-routes [routes]
  (-> (compojure-handler/site routes)))

(defrecord HTTP [port routes]
  ;; Implement the Lifecycle protocol
  component/Lifecycle

  (start [component]
    (println "Starting HTTP component on port" port)
    (assoc component :server (run-server
                              (get-routes (:routes routes))
                              {:port port :join? false})))

  (stop [component]
    (println "Stopping server")
    ((:server component) :timeout 100)
    (assoc component :server nil)))
