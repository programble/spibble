(ns spibble.views.login
  (:require [spibble.models.login :as login]
            [spibble.config :refer [api-key]]
            [noir.session :as session]
            [noir.response :refer [redirect]]
            [compojure.core :refer [defroutes GET]]))

(def auth-url (str "http://www.last.fm/api/auth/?api_key=" api-key))

(defroutes login-routes
  (GET "/login" []
    (when-not (session/get :user)
      (redirect auth-url)))

  (GET "/login/callback" [token]
    (when-let [user (and token (login/login token))]
      (session/put! :user user)
      (if (seq (:library user))
        (redirect (str "/library/" (:name user)))
        (redirect "/"))))

  (GET "/logout" []
    (session/remove! :user)
    (redirect "/")))
