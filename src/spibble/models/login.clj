(ns spibble.models.login
  (:require [spibble.utilities :refer [image-map]]
            [noir.session :as session]
            [monger.collection :as mc]
            [me.raynes.least :as least]
            [me.raynes.least.authorize :as auth]))

(def api-key "cd7f2e808a49a5b0bb21ec4250d054b5")
(def api-secret "42e961b131d5341778417199a6221f43")

(def auth-url (str "http://www.last.fm/api/auth/?api_key=" api-key))

(defn user-map [user info]
    (merge user
           (select-keys info [:realname :url])
           {:image (image-map (:image info))}))

(defn create-user [session]
  (let [user {:session (:key session)
              :name (:name session)
              :foo "penis"}]
    (mc/insert-and-return "users" user)))

(defn login [token]
  (let [session (auth/get-session api-key api-secret token)
        user (or (mc/find-one-as-map "users" {:session (:key session)})
                 (create-user session))
        info (:user (least/read "user.getInfo" api-key {:secret api-secret :sk (:session user)}))]
    (session/put! :user (user-map user info))))

(defn logout []
  (session/remove! :user))
