(ns spibble.server
  (:require [monger.core :as mg]
            [me.raynes.laser :as l]
            [compojure.core :refer [defroutes routes GET]]
            [compojure.route :refer [not-found resources]]
            [ring.middleware.params :refer [wrap-params]]
            [noir.session :refer [wrap-noir-flash wrap-noir-session]]
            [monger.ring.session-store :refer [monger-store]]
            [noir.util.middleware :refer [wrap-strip-trailing-slash]]))

(let [uri (get (System/getenv) "MONGOLAB_URI" "mongodb://127.0.0.1/spibble_development")]
  (mg/connect-via-uri! uri))

(require '[spibble.views.common :refer [template static layout]]
         '[spibble.views.login :refer [login-routes]]
         '[spibble.views.album :refer [album-routes]]
         '[spibble.views.library :refer [library-routes]])

(let [about (static :about)]
  (defroutes static-routes
    (GET "/about" []
      (layout about
              :active :about))))

(let [html (static :404)]
  (defn four-zero-four [req]
    (layout html)))

(def handler
  (-> (routes #'login-routes
              #'album-routes
              #'library-routes
              #'static-routes
              (resources "/")
              (not-found four-zero-four))
      (wrap-params)
      (wrap-noir-flash)
      (wrap-noir-session {:store (monger-store "sessions")})
      (wrap-strip-trailing-slash)))
