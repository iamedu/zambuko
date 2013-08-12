(ns zambuko-back.handler  
  (:require [zambuko-back.config :as config]
            [zambuko-back.routes.home  :refer [home-routes]]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [zambuko-back.routes.default :as zroute]
            [compojure.core :refer [defroutes]]            
            [noir.util.middleware :as middleware]
            [taoensso.timbre :as timbre]
            [com.postspectacular.rotor :as rotor]))

(defroutes app-routes
  (zroute/resources config/mongo-db config/bstore))

(defn init
  "init will be called once when
   app is deployed as a servlet on
   an app server such as Tomcat
   put any initialization code here"
  []
  (config/init!)

  (timbre/set-config!
    [:appenders :rotor]
    {:min-level :info
     :enabled? true
     :async? false ; should be always false for rotor
     :max-message-per-msecs nil
     :fn rotor/append})
  
  (timbre/set-config!
    [:shared-appender-config :rotor]
    {:path "zambuko_back.log" :max-size (* 512 1024) :backlog 10})
  
  (timbre/info "zambuko-back started successfully"))

(defn destroy
  "destroy will be called when your application
   shuts down, put any clean up code here"
  []
  (timbre/info "zambuko-back is shutting down...")
  (config/stop!))

(def app (middleware/app-handler
           ;;add your application routes here
           [app-routes home-routes]
           ;;add custom middleware here           
           :middleware [wrap-gzip]
           ;;add access rules here
           ;;each rule should be a vector
           :access-rules []))

(def war-handler (middleware/war-handler app))
