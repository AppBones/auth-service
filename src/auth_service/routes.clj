(ns auth-service.routes
  (:require [component.compojure :as ccompojure]
            [compojure.core :as compojure]
            [compojure.route :as route]
            [auth-service.handlers :refer :all]))

(ccompojure/defroutes Routes [db oauth]
  (compojure/context "/oauth2" []
    (compojure/POST "/token" request
      (post-token request db oauth))
    (compojure/GET "/authorize" request
      (get-authorize request db oauth))
    (compojure/POST "/authorize" request
      (post-authorize request db oauth))
    (route/not-found "The page could not be found")))
