(ns zambuko-back.config
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))

(def zambuko-config (atom nil))

(defn load-config []
  (let [stream (java.io.PushbackReader. (io/reader "server.edn"))]
    (edn/read stream)))
