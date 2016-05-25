(defproject auth-service "0.1.0-SNAPSHOT"
  :description "Auth Server for AppBone, powered by oauth.io"
  :url "http://github.com/AppBones/auth-service"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/data.codec "0.1.0"]
                 [org.clojure/data.json "0.2.6"]
                 [com.stuartsierra/component "0.3.1"]
                 [buddy "0.13.0"]
                 [compojure "1.5.0"]
                 [environ "1.0.2"]
                 [hiccup "1.0.5"]
                 [http-kit "2.1.19"]
                 [ring "1.4.0"]
                 [ring/ring-defaults "0.2.0"]
                 [valichek/component-compojure "0.2-SNAPSHOT"]]
  :plugins [[lein-ring "0.9.7"]
            [lein-environ "1.0.2"]]
  :profiles {:uberjar {:main auth-service.core
                       :uberjar-name "auth-service.jar"
                       :aot :all}
             :dev {:source-paths ["dev"]
                   :dependencies [[org.clojure/tools.namespace "0.2.10"]
                                  [javax.servlet/servlet-api "2.5"]
                                  [ring/ring-mock "0.3.0"]]
                   :env {:is-dev "true"}}})

