(ns spibble.views.library
  (:require [spibble.models.library :as library]
            [spibble.models.user :as user]
            [spibble.models.album :as album]
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

(defn add-page [id]
  (when-let [album (album/get-local id)]
    (let [updated (library/add-album (session/get :user) album)]
      (session/update-in! [:user] #(merge % updated))
      (redirect "/library"))))

(defn remove-page [id]
  (when-let [album (album/get-local id)]
    (let [updated (library/add-album (session/get :user) album)]
      (session/update-in! [:user] #(merge % updated))
      (redirect "/library"))))

(defroutes library-routes
  (GET "/library" []
    (when-let [user (session/get :user)]
      (redirect (str "/library/" (:name user)))))

  (GET "/library/:user" [user p]
    (when-let [p (if p (parse-pos-long p) 1)]
      (library-page user p)))

  (GET "/library/add/:id" [id]
    (when (session/get :user)
      (add-page (parse-pos-long id))))

  (GET "/library/remove/:id" [id]
    (when (session/get :user)
      (remove-page (parse-pos-long id)))))
