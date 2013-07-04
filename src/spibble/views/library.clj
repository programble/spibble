(ns spibble.views.library
  (:require [spibble.models.library :as library]
            [spibble.models.user :as user]
            [spibble.views.common :refer [template layout heading-search pager]]
            [spibble.views.album :refer [render-album-thumbs]]
            [spibble.utilities :refer [parse-pos-long count-pages]]
            [me.raynes.laser :as l :refer [defragment]]
            [noir.session :as session]
            [noir.response :refer [redirect]]
            [compojure.core :refer [defroutes GET]]))

(defn self? [user]
  (= (:name user) (session/get-in [:user :name])))

(defn library-page [name page]
  (when-let [user (user/get-local name)]
    (let [library (library/get-library user page 6)
          pages (count-pages (library/count-library user) 6)
          title (str name "'s Library")]
      (layout
        (concat (heading-search title (str "/library/" name "/search"))
                [(l/node :ul :attrs {:class "thumbnails"}
                         :content (render-album-thumbs library))]
                (pager (str "/library/" name "?") page pages))
        :title title
        :active (when (self? user) :library)))))

(defroutes library-routes
  (GET "/library/:user" [user p]
    (when-let [p (if p (parse-pos-long p) 1)]
      (library-page user p))))
