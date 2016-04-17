(ns auth-service.routes
  (:require [component.compojure :as ccompojure]
            [compojure.core :as compojure]
            [compojure.route :as route]
            [auth-service.handlers :refer :all]))

(ccompojure/defroutes Routes [db oauth]
  (compojure/context (str "/" (:basePath oauth)) []
    (compojure/GET "/token" request
      (token-fn request db oauth))
    (compojure/GET "/authorize" request
      (authorize-fn request db oauth))
    (compojure/POST "/authorize" request
      (authorize-fn request db oauth))
    (route/not-found "The page could not be found")))
