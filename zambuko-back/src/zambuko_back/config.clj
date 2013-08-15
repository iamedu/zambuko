(ns zambuko-back.config
  (:require [zambuko-back.consumer :as consumer]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [org.jclouds.blobstore2 :as bs]
            [monger.core :as mc]
            [clojure.tools.nrepl.server :as nrepl]
            [hornetq-clj.server :as server]
            [hornetq-clj.core-client :as client]
            [taoensso.timbre :as timbre]
            [classlojure.core :as cls]))

(defonce zambuko-config (atom nil))
(defonce bstore (atom nil))
(defonce nrepl-server (atom nil))
(defonce hornet-server (atom nil))
(defonce hornet-session-factory (atom nil))

(defn hornet-session []
  (.createSession @hornet-session-factory))

(defn setup-system! []
  (let [props (:system-properties @zambuko-config {})]
    (doseq [[k v] props]
      (System/setProperty k v))))

(defn start-blobstore! []
  (let [{:keys [backend username password]} (:blobstore @zambuko-config)
        containers ["sites" "tmp" "files" "classpath"]
        local-bstore (bs/blobstore backend username password)]
    (doseq [container containers]
      (bs/create-container local-bstore container))
    (reset! bstore local-bstore)))

(defn start-embedded-hornetq! [zc]
  (let [server (server/server {:in-vm true})]
    (reset! hornet-server server)
    (.start server)
    (client/in-vm-session-factory zc)))

(defn start-hornetq! []
  (let [{:keys [embedded] :as zc} (:hornetq @zambuko-config)]
    (do
      (reset! hornet-session-factory
              (if embedded
                (start-embedded-hornetq! zc)
                (client/netty-session-factory zc)))
      (let [session (hornet-session)]
        (try  
          (client/ensure-queue session "request-queue" {})
          (client/ensure-queue session "response-queue" {})
          (client/ensure-queue session "error-response-queue" {:durable true})
        (finally (.close session)))))))

(defn start-nrepl! []
  (let [{:keys [start port]} (:nrepl @zambuko-config)]
    (when start 
      (reset! nrepl-server (nrepl/start-server :port port)))))

(defn reload-consumers! []
  (try 
    (let [cl (zambuko.classloader.JCloudsClassLoader. cls/base-classloader)
          dcl  (clojure.lang.DynamicClassLoader. cl)]
      (with-bindings  {clojure.lang.Compiler/LOADER dcl}
        (cls/with-classloader cl
          (do
            (require '[zambuko.start :as start] :reload-all)
            (let [app (eval (read-string "start/app"))]
              (consumer/submit-handlers! app))))))
    (catch Exception e (timbre/error (.getMessage e))))) 

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
  (start-hornetq!)
  (start-nrepl!)
  (consumer/start! bstore hornet-session-factory)
  (reload-consumers!))

(defn stop! []
  (consumer/stop!)
  (stop-nrepl!)
  (stop-hornetq!))
