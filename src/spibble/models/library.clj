(ns spibble.models.library
  (:require [spibble.models.user :as user]
            [spibble.models.album :as album]
            [monger.collection :as mc]
            [monger.query :as mq]
            [monger.operators :refer [$push $pull $in]]))

(defn add-album [user album]
  (mc/update "users" {:name (:name user)} {$push {:library album}}))

(defn remove-album [user album]
  (mc/update "users" {:name (:name user)} {$pull {:library album}}))

(defn count-library [user]
  (count (:library user)))

(defn get-library [user page per]
  (map album/get-remote (mq/with-collection "albums"
                          (mq/find {:id {$in (vec (:library user))}})
                          ;; TODO: Sort by recent activity
                          (mq/paginate :page page :per-page per))))
