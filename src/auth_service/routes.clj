(ns auth-service.routes
  (:require [component.compojure :as ccompojure]
            [compojure.core :as compojure]
            [compojure.route :as route]
            [auth-service.handlers :refer :all]
            [auth-service.oauth :as oauth]))

(ccompojure/defroutes Routes [db oauth]
  (compojure/context "/oauth2" []
    (compojure/GET "/authorize" request
      (get-authorize request db oauth))
    (compojure/POST "/authorize" request
      (post-authorize request db oauth))
    (compojure/POST "/token" request
      (oauth/token request oauth))
    (compojure/GET "/check" request
      (oauth/check request oauth))
    (compojure/GET "/jwt" request
      (oauth/ac->jwt request oauth))
    (route/not-found "The page could not be found")))
