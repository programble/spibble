(ns spibble.models.login
  (:require [spibble.models.user :as user]
            [spibble.config :refer [api-key api-secret]]
            [monger.collection :as mc]
            [noir.session :as session]
            [me.raynes.least.authorize :as auth]))

(def auth-url (str "http://www.last.fm/api/auth/?api_key=" api-key))

(defn login [token]
  (when-let [session (auth/get-session api-key api-secret token)]
    (let [user (or (user/get-user (:name session))
                   (user/get-remote (user/add-user session)))]
      (session/put! :user user))))

(defn logout []
  (session/remove! :user))
