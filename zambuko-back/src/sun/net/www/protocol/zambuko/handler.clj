(ns sun.net.www.protocol.zambuko.handler
  (:require [org.jclouds.blobstore2 :as bs]
            [zambuko-back.config :as zc])
  (:import sun.net.www.protocol.zambuko.ZambukoConnection)
  (:gen-class
    :name sun.net.www.protocol.zambuko.Handler
    :extends java.net.URLStreamHandler
    :main false))

(defn -openConnection [this url]
  (ZambukoConnection. url))
