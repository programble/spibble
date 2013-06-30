(ns spibble.views.album
  (:require [spibble.models.album :as album]
            [spibble.views.common :refer [layout template pager]]
            [spibble.utilities :refer [safe-parse-long]]
            [noir.session :as session]
            [compojure.core :refer [defroutes GET]]
            [noir.response :refer [redirect]]
            [me.raynes.laser :refer [defragment] :as l]))

(defn album-thumb [node album]
  (let [{:keys [name artist id image]} album]
    (l/at node
          (l/element= :a) (l/attr :href (str "/album/" id))
          (l/element= :img) (l/attr :src (:extralarge image))
          (l/class= :album) (l/content name)
          (l/element= :h4) (l/content artist))))

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

(defn albums-page []
  (layout
    (albums (album/get-top-albums))
    {:active :albums}))

(defn search-page [query page]
  (if (empty? query)
    (redirect "/albums")
    (let [{:keys [albums pages]} (album/search query page)]
      (layout
        (conj (album-search query albums)
              (pager (str "/search?q=" query) page pages))
        {:title (str "Album search: " query)}))))

(defroutes album-routes
  (GET "/" []
    (if (session/get :user)
      (redirect "/library")
      (albums-page)))

  (GET "/albums" []
    (albums-page))

  (GET "/search" [q p]
    (search-page q (safe-parse-long p 1))))
