(ns spibble.server
  (:require [me.raynes.laser :as l]
            [noir.util.middleware :refer [wrap-strip-trailing-slash]]
            [ring.middleware.params :refer [wrap-params]]
            [noir.session :refer [wrap-noir-session wrap-noir-flash] :as session]
            [monger.core :as mg]
            [monger.collection :as mc]
            [monger.ring.session-store :refer [monger-store]]
            [compojure.core :refer [defroutes routes GET]]
            [compojure.route :refer [not-found resources]]))

(let [uri (get (System/getenv) "MONGOLAB_URI" "mongodb://127.0.0.1/spibble_development")]
  (mg/connect-via-uri! uri))

(require '[spibble.views.common :refer [layout template]]
         '[spibble.views.login :refer [login-routes]]
         '[spibble.views.album :refer [album-routes]])

(let [html (-> (template :404) slurp l/unescaped)]
  (defn four-zero-four [req]
    (layout html)))

(def handler
  (-> (routes login-routes
              album-routes
              (resources "/")
              (not-found four-zero-four))
      (wrap-params)
      (wrap-noir-flash)
      (wrap-noir-session {:store (monger-store "sessions")})
      (wrap-strip-trailing-slash)))
