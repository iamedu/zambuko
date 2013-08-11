(ns zambuko-back.routes.default
  (:require [compojure.response :as response]
            [ring.middleware.file-info :as f]
            [ring.middleware.content-type :as c]
            [ring.middleware.head :as h]
            [ring.util.response :as r]
            [org.jclouds.blobstore2 :as bs]
            [zambuko-back.data.sites :as sites]))

(defn resources [db bstore]
  (fn [request]
    (let [host (get-in request [:headers "host"] "default")
          uri  (get request :uri)
          site (sites/site @db host)
          site-id (:_id site "default")
          full-path (str site-id uri)
          stream (and (bs/blob-exists? @bstore "sites" full-path)
                      (bs/get-blob-stream @bstore "sites" full-path))]
      (if stream
        (let [stream-fn (-> stream
                            (r/response)
                            (constantly)
                            (c/wrap-content-type)
                            (f/wrap-file-info)
                            (h/wrap-head))]
          (stream-fn request))))))

