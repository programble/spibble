(ns spibble.views.login
  (:require [spibble.models.login :as login]
            [noir.session :as session]
            [compojure.core :refer [defroutes GET]]
            [noir.response :refer [redirect]]))

(defroutes login-routes
  (GET "/login" []
    (if (session/get :user)
      (redirect "/")
      (redirect login/auth-url)))

  (GET "/login/callback" [token]
    (login/login token)
    (redirect "/"))

  (GET "/logout" []
    (login/logout)
    (redirect "/")))
