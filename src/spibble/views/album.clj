(ns spibble.views.album
  (:require [clojure.string :refer [join]]
            [spibble.models.album :as album]
            [spibble.views.common :refer [template static layout heading-search pager]]
            [spibble.utilities :refer [parse-pos-long count-pages pluralize]]
            [me.raynes.laser :as l :refer [defragment]]
            [noir.session :as session]
            [noir.response :refer [redirect]]
            [compojure.core :refer [defroutes GET]]))

(defn format-artists [album]
  (join ", " (map #(-> % :artist :name) (:artist-credit album))))

(defn format-labels [album]
  (join ", " (map #(-> % :label :name) (:label-info album))))

(def format-map
  {"12\" Vinyl" "12″"
   "10\" Vinyl" "10″"
   "7\" Vinyl" "7″"})

(defn format-media [album]
  (let [media (frequencies (map (comp format-map :format) (:media album)))]
    (join ", " (for [[m n] media]
                 (if (= n 1) m (str n "×" m))))))

(defragment render-api-error (template :api-error)
  [error]
  [(l/class= :api-error) (l/content (:error error))])

(defn album-thumb-node [node album]
  (if (:error album)
    (l/at node
          [(l/class= :media) (l/content (render-api-error album))])
    (l/at node
          [(l/element= :a) (l/attr :href (str "/album/" (:id album)))]
          [(l/element= :img) (l/attr :src (str (-> album :image :extralarge)))]
          [(l/class= :album-title) (l/content (:title album))]
          [(l/class= :album-artist) (l/content (format-artists album))]
          [(l/class= :album-label) (l/content (format-labels album))]
          [(l/class= :album-media) (l/content (format-media album))]
          [(l/class= :owners) (l/content (pluralize (get album :owners 0) "owner"))])))

(let [none-html (static :none)]
  (defragment render-album-thumbs (template :album-thumb)
    [albums]
    (if (seq albums)
      [(l/element= :li) #(for [album albums]
                           (album-thumb-node % album))]
      [(l/element= :li) (comp (l/attr :class "span12")
                              (l/content none-html))])))

(defn render-tracks-table [album]
  (for [track (apply concat (map :tracks (:media album)))]
    (l/node :tr :content [(l/node :td :content (:number track))
                          (l/node :td :content (:title track))
                          ;; TODO: Format duration
                          (l/node :td :content (str (:length track)))])))

(defragment render-album (template :album)
  [{:keys [id title] :as album}]
  (if-let [user (session/get :user)]
    (if (some #{id} (:library user))
      (l/compose-pews
        [(l/id= :library-remove) (l/attr :href (str "/library/remove/" id))]
        [(l/id= :library-add) (l/remove)])
      (l/compose-pews
        [(l/id= :library-add) (l/attr :href (str "/library/add/" id))]
        [(l/id= :library-remove) (l/remove)]))
    [(l/class= :logged-in) (l/remove)])
  [(l/element= :img) (l/attr :src (str (-> album :image :extralarge)))]
  [(l/id= :album-title) (l/content title)]
  [(l/id= :album-artist) (l/content (format-artists album))]
  [(l/id= :album-media) (l/content (format-media album))]
  [(l/id= :album-label) (l/content (format-labels album))]
  [(l/element= :table) (l/content (render-tracks-table album))])

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
        :title (:title album)
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
    (album-page (parse-pos-long id)))

  (GET "/album/mbid/:mbid" [mbid]
    (when-let [album (album/get-album-by-mbid mbid)]
      (redirect (str "/album/" (:id album))))))
