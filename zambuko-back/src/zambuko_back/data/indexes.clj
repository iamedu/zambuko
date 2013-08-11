(ns zambuko-back.data.indexes
  (:require [monger.collection :as mc]))

(defn create-indexes []
  (mc/ensure-index "sites"  (array-map :site-name 1) { :unique true }))
