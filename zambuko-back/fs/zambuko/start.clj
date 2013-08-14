(ns zambuko.start
  (:require [compojure.core :refer [defroutes GET]]
            [noir.util.middleware :as middleware]))


(defroutes app-routes
  (GET "/hello" [] "<h1>Hello world"))

(def app (middleware/app-handler
           [app-routes]
           :middleware []
           :access-rules []))
