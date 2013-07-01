(ns spibble.views.login
  (:require [spibble.models.login :as login]
            [noir.session :as session]
            [noir.response :refer [redirect]]
            [compojure.core :refer [defroutes GET]]))

(defroutes login-routes
  (GET "/login" []
    (if (session/get :user)
      (redirect "/")
      (redirect login/auth-url)))

  (GET "/login/callback" [token]
    (when token
      (login/login token)
      (redirect "/")))

  (GET "/logout" []
    (login/logout)
    (redirect "/")))
