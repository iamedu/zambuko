(ns zambuko-back.data.sites
  (:require [zambuko-back.config :as zc]
            [org.jclouds.blobstore2 :as bs]
            [monger.collection :as mc]
            [monger.core :as mg]
            [monger.util :as mu]
            [validateur.validation :as v]))

(def ^:const schema-version 1)

(def site-validation-set (v/validation-set
                           (v/presence-of :site-name)
                           (v/presence-of :description)))

(def site-valid?
  (partial v/valid? site-validation-set))

(defn sites [db]
  (mg/with-db db
    (mc/find-maps "sites")))

(defn site [db site-name]
  (mg/with-db db
    (mc/find-one-as-map "sites" {:site-name site-name})))

(defn create-site [db & {:as site}]
  (mg/with-db db
    (let [uuid (mu/random-uuid)
          new-site (assoc site :_id uuid :schema-version schema-version :version 0)]
      (if (site-valid? new-site)
        (let [created-site (mc/insert-and-return "sites" new-site)]
          (bs/create-directory @zc/bstore "sites" (str uuid "/"))
          created-site)))))

(defn delete-site [db site-name]
  (mg/with-db db
    (let [site (site db site-name)]
     (when site
       (bs/delete-directory @zc/bstore "sites" (:_id site))
       (bean (mc/remove "sites" { :site-name site-name }))))))

(defn update-site [db site-name & {:as data}]
  (let [merged (assoc data :site-name site-name)]
    (mg/with-db db
      (when (site-valid? merged)
        (mc/update "sites" {:site-name site-name} merged :multi false)
        (site db site-name)))))

