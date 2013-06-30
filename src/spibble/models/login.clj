(ns spibble.models.login
  (:require [spibble.utilities [image-map]]
            [spibble.config :refer [api-key api-secret]]
            [monger.collection :as mc]
            [noir.session :as session]
            [me.raynes.least :as least]
            [me.raynes.least.authorize :as auth]))

(def auth-url (str "http://www.last.fm/api/auth/?api_key=" api-key))

(mc/ensure-index "users" {:session 1})

(defn from-lastfm [user]
  (-> user
      (select-keys [:realname :url])
      (assoc :image (image-map (:image user)))))

(defn create-user [session]
  (let [user {:session (:key session)
              :name (:name session)}]
    (mc/insert-and-return "users" user)))

(defn login [token]
  (let [session (auth/get-session api-key api-secret token)
        user (or (mc/find-one-as-map "users" {:session (:key session)})
                 (create-user session))
        info (:user (least/read "user.getInfo" api-key {:secret api-secret, :sk (:session user)}))]
    (session/put! :user (merge user (from-lastfm info)))))

(defn logout []
  (session/remove! :user))
