(ns spibble.models.library
  (:require [spibble.models.user :as user]
            [spibble.models.album :as album]
            [monger.collection :as mc]
            [monger.query :as mq]
            [monger.operators :refer [$addToSet $pull $in $inc $set]]))

(defn add-album [user album]
  (mc/update "albums"
             {:id (:id album)}
             {$inc {:owners 1}
              $set {(str "activity." (:name user)) (java.util.Date.)}})
  (mc/find-and-modify "users"
                      {:name (:name user)}
                      {$addToSet {:library (:id album)}}
                      :return-new true))

(defn remove-album [user album]
  (mc/update "albums" {:id (:id album)} {$inc {:owners -1}})
  (mc/find-and-modify "users"
                      {:name (:name user)}
                      {$pull {:library (:id album)}}
                      :return-new true))

(defn count-library [user]
  (count (:library user)))

(defn get-library [user page per]
  (map album/get-remote (mq/with-collection "albums"
                          (mq/find {:id {$in (vec (:library user))}})
                          (mq/sort {(str "activity." (:name user)) -1})
                          (mq/paginate :page page :per-page per))))
