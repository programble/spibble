(ns spibble.views.library
  (:require [spibble.models.library :as library]
            [spibble.models.user :as user]
            [spibble.views.common :refer [template layout heading-search pager]]
            [spibble.views.album :refer [album-thumbs]]
            [spibble.utilities :refer [safe-parse-long count-pages]]
            [me.raynes.laser :as l :refer [defragment]]
            [noir.session :as session]
            [noir.response :refer [redirect]]
            [compojure.core :refer [defroutes GET]]))

(defn self? [user]
  (= (:name user )(session/get-in [:user :name])))

(defn library-page [name page]
  (when-let [user (user/get-local name)]
    (let [title (str name "'s Library")]
      (layout
        (concat (heading-search title (str "/library/" name "/search"))
                [(l/node :ul :attrs {:class "thumbnails"}
                         :content (album-thumbs (library/get-library user page 6)))]
                (pager (str "/library/" name) page (count-pages (library/count-library user) 6)))
        :title title
        :active (when (self? user) :library)))))

(defroutes library-routes
  (GET "/library/:user" [user p]
    (library-page user (safe-parse-long p 1))))
