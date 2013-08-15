(ns zambuko-back.consumer
  (:require [hornetq-clj.core-client :as hq]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [taoensso.timbre :as timbre]) 
  (:import [java.util.concurrent Executors]))

(defonce worker-pool (atom nil))
(defonce handler (atom nil))
(defonce bstore (atom nil))
(defonce hornet-session-factory (atom nil))

(def threads 16)
(def listeners 32)

(defn save-error [m exception session error-producer]
  (let [uuid (.getStringProperty m "uuid")
        message (.getStringProperty m "edn-message")
        body (.getBytesProperty m "body")
        string-writer (java.io.StringWriter.)
        print-writer (java.io.PrintWriter. string-writer)]
    (do
      (.printStackTrace exception print-writer)
      (hq/send-message error-producer (doto (hq/create-message session true)
                                  (.putStringProperty "uuid" uuid)
                                  (.putStringProperty "request-message" message)
                                  (.putBytesProperty "request-body" body)
                                  (.putIntProperty "delivery-count" (.getDeliveryCount m))
                                  (.putStringProperty "error-message" (.getMessage exception))
                                  (.putStringProperty "exception" (-> string-writer .getBuffer .toString)))))))

(defn handle-error [m exception session producer]
  (let [uuid (.getStringProperty m "uuid")]
    (do
      (hq/send-message producer (doto (hq/create-message session false)
                                  (.putStringProperty "uuid" uuid)
                                  (.putStringProperty "error-message" (.getMessage exception))
                                  (.putBooleanProperty "served" true)
                                  (.putBooleanProperty "error" true))))))

(defn handle-message [m session producer error-producer]
  (let [uuid (.getStringProperty m "uuid")
        message (edn/read-string (.getStringProperty m "edn-message"))
        body (java.io.ByteArrayInputStream. (.getBytesProperty m "body"))
        request (assoc message :body body)]
    (do 
      (try 
        (let [response (@handler request)
              edn-message (pr-str (dissoc response :body))
              output-stream (java.io.ByteArrayOutputStream.)
              plain-response (:body response)]
          (when response
            (io/copy plain-response output-stream)
            (hq/send-message producer (doto (hq/create-message session false)
                                        (.putStringProperty "uuid" uuid)
                                        (.putStringProperty "edn-message" edn-message)
                                        (.putBooleanProperty "served" true)
                                        (.putBytesProperty "body" (.toByteArray output-stream)))))
          (when-not response
            (hq/send-message producer (doto (hq/create-message session false)
                                        (.putStringProperty "uuid" uuid)
                                        (.putBooleanProperty "served" false)))))
        (catch Exception e
          (do
            (save-error m e session error-producer)   
            (handle-error m e session producer))))
      (hq/acknowledge m))))

(defn look-for-work []
  (let [task (fn []
               (do 
                 (try 
                   (let [session (.createSession @@hornet-session-factory) 
                         consumer (hq/create-consumer session "request-queue" {})
                         producer (hq/create-producer session "response-queue") 
                         error-producer (hq/create-producer session "error-queue")]
                     (try
                       (do
                         (.start session)
                         (handle-message (hq/receive-message consumer) session producer error-producer))
                       (finally (.close session))))
                   (catch Exception e (timbre/error e "Cannot consume request message")))
                 (look-for-work)))]
    (.submit @worker-pool task)))

(defn start! [b h]
  (do
    (reset! bstore b)
    (reset! hornet-session-factory h)
    (reset! worker-pool (Executors/newFixedThreadPool threads))
    (dotimes [_ listeners] (look-for-work))))

(defn stop! []
  (.shutdownNow @worker-pool))

(defn submit-handlers! [app]
  (reset! handler app))
