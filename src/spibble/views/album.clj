(ns spibble.views.album
  (:require [spibble.models.album :as album]
            [spibble.views.common :refer [template static layout heading-search pager]]
            [spibble.utilities :refer [parse-pos-long count-pages]]
            [me.raynes.laser :as l :refer [defragment]]
            [noir.session :as session]
            [noir.response :refer [redirect]]
            [compojure.core :refer [defroutes GET]]))

(defragment render-api-error (template :api-error)
  [error]
  (l/class= :api-error) (l/content (:message error)))

(defn album-thumb-node [node {:keys [name artist id image] :as album}]
  (l/at node
        (if (:error album)
          [(l/class= :media) (l/content (render-api-error album))]
          [(l/element= :a) (l/attr :href (str "/album/" id))
           (l/class= :album) (l/content name)
           (l/element= :h4) (l/content artist)
           (l/element= :img) (l/attr :src (:extralarge image))])))

(let [none-html (static :none)]
  (defragment render-album-thumbs (template :album-thumb)
    [albums]
    (l/element= :li) (if (seq albums)
                       #(for [album albums]
                          (album-thumb-node % album))
                       (comp (l/attr :class "span12")
                             (l/content none-html)))))

(defn render-tracks-table [tracks]
  (for [track tracks]
    (l/node :tr :content [(l/node :td :content (:name track))
                          ;; TODO: Format duration
                          (l/node :td :content (str (:duration track)))])))

(defragment render-album (template :album)
  [{:keys [name artist image tracks]}]
  (l/element= :h1) (l/content name)
  (l/element= :h2) (l/content artist)
  (l/element= :img) (l/attr :src (:extralarge image))
  (l/element= :table) (l/content (render-tracks-table tracks)))

(let [hero-html (static :hero-unit)
      heading (heading-search "Top Albums" "/albums/search")]
  (defn top-albums-page [page]
    (let [albums (album/get-top-albums page 6)
          pages (count-pages (album/count-albums) 6)]
      (layout
        (concat (when-not (session/get :user)
                  [hero-html])
                heading
                [(l/node :ul :attrs {:class "thumbnails"}
                         :content (render-album-thumbs albums))]
                (pager "/albums?" page pages))
        :active :albums))))

(defn search-page [query page]
  (let [{:keys [albums pages]} (album/search query page 6)
        title (str "Album search: " query)]
    (layout
      (concat (heading-search title "/albums/search" query)
              [(l/node :ul :attrs {:class "thumbnails"}
                       :content (render-album-thumbs albums))]
              (pager (str "/albums/search?q=" query "&") page pages))
      :title title
      :active :albums)))

(defn album-page [id]
  (when-let [album (album/get-album id)]
    (if (:error album)
      (layout (render-api-error album) :active :albums)
      (layout
        (render-album album)
        :title (:name album)
        :active :albums))))

(defroutes album-routes
  (GET "/" []
    (top-albums-page 1))

  (GET "/albums" [p]
    (when-let [p (if p (parse-pos-long p) 1)]
      (top-albums-page p)))

  (GET "/albums/search" [q p]
    (when-let [p (if p (parse-pos-long p) 1)]
      (when (seq q)
        (search-page q p))))

  (GET "/album/:id" [id]
    (album-page (parse-pos-long id))))
