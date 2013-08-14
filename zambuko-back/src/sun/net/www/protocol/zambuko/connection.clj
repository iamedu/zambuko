(ns sun.net.www.protocol.zambuko.connection
  (:require [org.jclouds.blobstore2 :as bs]
            [zambuko-back.config :as zc])
  (:import (java.io FileNotFoundException))
  (:gen-class
    :name sun.net.www.protocol.zambuko.ZambukoConnection
    :extends java.net.URLConnection
    :main false))

(defn normalize-name [name]
  (apply str (drop-while #(= \/ %) name)))

(defn -connect [this]
  (let [file-name (normalize-name (-> this .getURL .getFile))]
    (if-not (bs/blob-exists? @zc/bstore "classpath" file-name)
      (throw (FileNotFoundException. file-name)))))

(defn -getInputStream [this]
  (let [file-name (normalize-name (-> this .getURL .getFile))]
    (if (bs/blob-exists? @zc/bstore "classpath" file-name)
      (bs/get-blob-stream @zc/bstore "classpath" file-name)
      (throw (FileNotFoundException. file-name)))))

(defn -getContent [this]
  (-getInputStream this))
