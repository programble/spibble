(ns spibble.views.login
  (:require [spibble.models.user :as user]
            [spibble.config :refer [api-key]]
            [noir.session :as session]
            [noir.response :refer [redirect]]
            [compojure.core :refer [defroutes GET]]))

(def auth-url (str "http://www.last.fm/api/auth/?api_key=" api-key))

(defroutes login-routes
  (GET "/login" {:keys [headers]}
    (when-not (session/get :user)
      (when-let [referer (get headers "referer")]
        (let [from (.getFile (java.net.URL. (get headers "referer")))]
          (when-not (= from "/")
            (session/flash-put! :from from))))
      (redirect auth-url)))

  (GET "/login/callback" [token]
    (when-let [user (and token (user/login token))]
      (session/put! :user user)
      (if-let [from (session/flash-get :from)]
        (redirect from)
        (if (seq (:library user))
          (redirect (str "/library/" (:name user)))
          (redirect "/")))))

  (GET "/logout" {:keys [headers]}
    (session/remove! :user)
    (if-let [referer (get headers "referer")]
      (let [from (.getFile (java.net.URL. referer))]
        (redirect from))
      (redirect "/"))))
