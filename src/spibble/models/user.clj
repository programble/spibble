(ns spibble.models.user
  (:require [spibble.config :refer [api-key api-secret]]
            [spibble.utilities :refer [lastfm image-from-lastfm]]
            [monger.collection :as mc]
            [me.raynes.least.authorize :as auth]))

(mc/ensure-index "users" {:name 1})

(defn from-lastfm [user]
  (-> user
      (select-keys [:realname :url])
      (assoc :image (image-from-lastfm (:image user)))))

(defn get-local [name]
  (mc/find-one-as-map "users" {:name name}))

(defn get-remote [local]
  (let [resp (lastfm "user.getInfo" {:user (:name local)})]
    (if (:error resp)
      resp
      (merge (from-lastfm (:user resp)) local))))

(defn get-user [name]
  (when-let [local (get-local name)]
    (get-remote local)))

(defn login [token]
  (when-let [session (auth/get-session api-key api-secret token)]
    (or (get-user (:name session))
        (get-remote (mc/insert-and-return "users"
                                          {:session (:key session)
                                           :name (:name session)})))))
