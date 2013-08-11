(ns zambuko-back.config
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [org.jclouds.blobstore2 :as bs]
            [monger.core :as mc]))

(def zambuko-config (atom nil))
(def bstore (atom nil))

(defn setup-system! []
  (let [props (:system-properties @zambuko-config {})]
    (doseq [[k v] props]
      (System/setProperty k v))))

(defn start-blobstore! []
  (let [{:keys [backend username password]} (:blobstore @zambuko-config)]
    (reset! bstore (bs/blobstore backend username password))))

(defn start-mongo! []
  (let [{:keys [host port username password db]} (:mongo @zambuko-config)]
    (mc/connect! {:host host :port port})
    (mc/use-db! db)
    (mc/authenticate (mc/get-db db) username (.toCharArray password))))

(defn start-hornetq! []
  (let [{:keys [embedded]} (:hornetq @zambuko-config)]))

(defn load-config []
  (with-open [stream (java.io.PushbackReader. (io/reader "server.edn"))]
    (edn/read stream)))

(defn set-config! [config]
  (reset! zambuko-config (config)))

(defn init! []
  (reset! zambuko-config (load-config))
  (setup-system!)
  (start-blobstore!)
  (start-mongo!)
  (start-hornetq!))

