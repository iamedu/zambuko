(ns zambuko-back.routes.default
  (:require [compojure.response :as response]
            [ring.middleware.file-info :as f]
            [ring.middleware.content-type :as c]
            [ring.middleware.head :as h]
            [ring.util.response :as r]
            [ring.middleware.gzip :as gz]
            [org.jclouds.blobstore2 :as bs]
            [noir.util.cache :as cache]))

(defn- default-index [request]
  (if (= (get request :uri) "/")
    (assoc request :uri "/index.html")
    request))

(defn resources [db bstore]
  (fn [original-request]
    (let [request (default-index original-request)
          host (get-in request [:headers "host"] "default")
          uri  (get request :uri)
          site-id (if (bs/directory-exists? @bstore "sites" host) host "default")
          full-path (str site-id uri)
          stream-fn #(bs/get-blob-stream @bstore "sites" full-path)
          stream (cache/cache! (str "sites/" full-path) stream-fn)]
      (if (bs/blob-exists? @bstore "sites" full-path)
        (let [request-fn (-> (stream)
                             (r/response)
                             (constantly)
                             (c/wrap-content-type)
                             (f/wrap-file-info)
                             (h/wrap-head)
                             (gz/wrap-gzip))]
          (request-fn request))))))

