(ns zambuko-back.routes.queue
  (:require [zambuko-back.config :as zc]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [hornetq-clj.core-client :as hq])
  (:import [org.hornetq.api.core Message]))

(defn handle-response [consumer]
  (when-let [message (hq/receive-message consumer 5000)]
    (if (.getBooleanProperty message "served")
      (let [response (edn/read-string (.getStringProperty message "edn-message"))
            body (java.io.ByteArrayInputStream. (.getBytesProperty message "body"))]
        (assoc response :body body)))))

(defn publish-request []
  (fn [{:keys [body] :as request}]
    (let [uuid (str (java.util.UUID/randomUUID))
          edn-message (pr-str (dissoc request :body))
          output-stream (java.io.ByteArrayOutputStream.)
          session (zc/hornet-session)
          producer (hq/create-producer session "request-queue")
          consumer (hq/create-consumer session "response-queue" {:filter (str "uuid = '" uuid "'")})]
      (try 
        (do
          (io/copy body output-stream)
          (hq/send-message producer (doto (hq/create-message session false)
                                      (.putStringProperty "uuid" uuid)
                                      (.putStringProperty "edn-message" edn-message)
                                      (.putBytesProperty "body" (.toByteArray output-stream))))
          (.start session)
          (handle-response consumer))
        (finally (.close session))))))

