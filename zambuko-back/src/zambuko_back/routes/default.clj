(ns zambuko-back.routes.default
  (:require [compojure.response :as response]))

(defn resources [db bstore]
  (fn [request]
    (let [host (get-in request [:headers "host"] "default")]
      (println host)
      (response/render "Hola" request))))


