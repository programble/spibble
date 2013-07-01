(ns spibble.views.album
  (:require [spibble.models.album :as album]
            [spibble.views.common :refer [template layout pager]]
            [spibble.utilities :refer [safe-parse-long count-pages]]
            [me.raynes.laser :as l :refer [defragment]]
            [noir.session :as session]
            [noir.response :refer [redirect]]
            [compojure.core :refer [defroutes GET]]))

(defragment api-error (template :api-error)
  [error]
  (l/class= :api-error) (l/content (:message error)))

(defn album-thumb [node {:keys [name artist id image] :as album}]
  (l/at node
        (if (:error album)
          [(l/class= :media) (l/content (api-error album))]
          [(l/element= :a) (l/attr :href (str "/album/" id))
           (l/element= :img) (l/attr :src (:extralarge image))
           (l/class= :album) (l/content name)
           (l/element= :h4) (l/content artist)])))

(defragment album-thumbs (template :album-thumb)
  [albums]
  (l/element= :li) #(for [album albums]
                      (album-thumb % album)))

(defragment albums (template :albums)
  [albums]
  (when (session/get :user)
    [(l/class= :hero-unit) (l/remove)])
  (l/class= :thumbnails) (l/content (album-thumbs albums)))

(defragment album-search (template :search)
  [query albums]
  (l/class= :thumbnails) (l/content (album-thumbs albums))
  (l/element= :em) (l/content query))

(defn tracks-table [tracks]
  (for [track tracks]
    (l/node :tr
            :content [(l/node :td :content (:name track))
                      (l/node :td :content (str (:duration track)))]))) ; TODO: Format duration

(defragment show-album (template :album)
  [album]
  (l/element= :img) (l/attr :src (-> album :image :extralarge))
  (l/element= :h1) (l/content (:name album))
  (l/element= :h2) (l/content (:artist album))
  (l/element= :table) (l/content (tracks-table (:tracks album))))

(defn albums-page [page]
  (layout
    (conj (albums (album/get-top-albums page 6))
          (pager "/albums?" page (count-pages (album/count-albums) 6)))
    :active :albums))

(defn search-page [query page]
  (if (seq query)
    (let [{:keys [albums pages]} (album/search query page 6)]
      (layout (conj (album-search query albums)
                    (pager (str "/search?q=" query "&") page pages))
              :title (str "Album search: " query)
              :query query))
    (redirect "/albums")))

(defn album-page [id]
  (when-let [album (album/get-album id)]
    (if (:error album)
      (layout (api-error album))
      (layout
        (show-album album)
        :title (:name album)))))

(defroutes album-routes
  (GET "/" []
    (if (session/get :user)
      (redirect "/library")
      (albums-page 1)))

  (GET "/albums" [p]
    (albums-page (safe-parse-long p 1)))

  (GET "/search" [q p]
    (search-page q (safe-parse-long p 1)))

  (GET "/album/:id" [id]
    (album-page (safe-parse-long id))))
