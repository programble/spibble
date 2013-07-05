(ns spibble.models.library
  (:require [spibble.models.user :as user]
            [spibble.models.album :as album]
            [monger.collection :as mc]
            [monger.query :as mq]
            [monger.operators :refer [$addToSet $pull $in]]))

(defn add-album [user album]
  (mc/find-and-modify "users"
                      {:name (:name user)}
                      {$addToSet {:library (:id album)}}
                      :return-new true))

(defn remove-album [user album]
  (mc/find-and-modify "users"
                      {:name (:name user)}
                      {$pull {:library (:id album)}}
                      :return-new true))

(defn count-library [user]
  (count (:library user)))

(defn get-library [user page per]
  (map album/get-remote (mq/with-collection "albums"
                          (mq/find {:id {$in (vec (:library user))}})
                          ;; TODO: Sort by recent activity
                          (mq/paginate :page page :per-page per))))
