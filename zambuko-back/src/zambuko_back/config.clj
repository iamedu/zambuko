(ns zambuko-back.config
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [org.jclouds.blobstore2 :as bs]
            [monger.core :as mc]
            [clojure.tools.nrepl.server :as nrepl]
            [hornetq-clj.server :as server]
            [hornetq-clj.core-client :as client]))

(def zambuko-config (atom nil))
(def bstore (atom nil))
(def nrepl-server (atom nil))
(def hornet-server (atom nil))
(def hornet-session-factory (atom nil))

(defn setup-system! []
  (let [props (:system-properties @zambuko-config {})]
    (doseq [[k v] props]
      (System/setProperty k v))))

(defn start-blobstore! []
  (let [{:keys [backend username password]} (:blobstore @zambuko-config)]
    (reset! bstore (bs/blobstore backend username password))))

(defn start-mongo! []
  (let [{:keys [username password db] :as zc} (:mongo @zambuko-config)]
    (mc/connect! zc)
    (mc/use-db! db)
    (mc/authenticate (mc/get-db db) username (.toCharArray password))))

(defn start-embedded-hornetq! [zc]
  (let [server (server/server {:in-vm true})]
    (reset! hornet-server server)
    (.start server)
    (client/in-vm-session-factory zc)))

(defn start-hornetq! []
  (let [{:keys [embedded] :as zc} (:hornetq @zambuko-config)]
    (reset! hornet-session-factory
            (if embedded
              (start-embedded-hornetq! zc)
              (client/netty-session-factory zc)))))

(defn start-nrepl! []
  (let [{:keys [start port]} (:nrepl @zambuko-config)]
    (when start 
      (reset! nrepl-server (nrepl/start-server :port port)))))

(defn stop-hornetq! []
  (when @hornet-server
    (.stop hornet-server)))

(defn stop-nrepl! []
  (when @nrepl-server 
    (nrepl/stop-server @nrepl-server)))

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
  (start-hornetq!)
  (start-nrepl!))

(defn stop! []
  (stop-nrepl!)
  (stop-hornetq!))
