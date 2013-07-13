(ns spibble.views.library
  (:require [spibble.models.library :as library]
            [spibble.models.user :as user]
            [spibble.models.album :as album]
            [spibble.views.common :refer [template ajax? layout heading-search pager]]
            [spibble.views.album :refer [render-album-thumbs render-album-buttons]]
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
          count (library/count-library user)
          pages (count-pages count 6)
          title (str name "'s Library")
          count-node (l/node :small :content (str "(" count ")"))]
      (layout
        (concat (heading-search [title " " count-node]
                                (str "/library/" name "/search"))
                [(l/node :ul :attrs {:class "thumbnails"}
                         :content (render-album-thumbs library))]
                (pager (str "/library/" name "?") page pages))
        :title title
        :active (when (self? user) :library)))))

(defn add-page [req id]
  (when-let [album (album/get-album id)]
    (let [updated (library/add-album (session/get :user) album)]
      (session/update-in! [:user] #(merge % updated))
      (if (ajax? req)
        (l/fragment-to-html (render-album-buttons album))
        (redirect "/library")))))

(defn remove-page [req id]
  (when-let [album (album/get-album id)]
    (let [updated (library/remove-album (session/get :user) album)]
      (session/update-in! [:user] #(merge % updated))
      (if (ajax? req)
        (l/fragment-to-html (render-album-buttons album))
        (redirect "/library")))))

(defroutes library-routes
  (GET "/library" []
    (when-let [user (session/get :user)]
      (redirect (str "/library/" (:name user)))))

  (GET "/library/:user" [user p]
    (when-let [p (if p (parse-pos-long p) 1)]
      (library-page user p)))

  (GET "/library/add/:id" [id :as req]
    (when (session/get :user)
      (add-page req (parse-pos-long id))))

  (GET "/library/remove/:id" [id :as req]
    (when (session/get :user)
      (remove-page req (parse-pos-long id)))))
