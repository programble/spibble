(ns spibble.views.album
  (:require [clojure.string :refer [join]]
            [spibble.models.album :as album]
            [spibble.views.common :refer [template static layout heading-search pager]]
            [spibble.utilities :refer [parse-pos-long count-pages pluralize format-ms]]
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

(defragment render-album-buttons (template :album-buttons)
  [{:keys [id]} & [small]]
  (if-let [user (session/get :user)]
    (l/compose-pews
      (if (some #{id} (:library user))
        (l/compose-pews
          [(l/class= :library-remove) (l/attr :href (str "/library/remove/" id))]
          [(l/class= :library-add)    (l/remove)])
        (l/compose-pews
          [(l/class= :library-add)    (l/attr :href (str "/library/add/" id))]
          [(l/class= :library-remove) (l/remove)]))
      (when small
        (l/compose-pews
          [(l/element= :span) (l/remove)]
          [(l/element= :a)    (comp (l/remove-class "btn-block")
                                    (l/remove-class "btn-left")
                                    (l/add-class "btn-small"))])))
    [(l/element= :a) (l/remove)]))

(defragment render-album-thumb (template :album-thumb)
  [album]
  (if (:error album)
    [(l/class= :media) (l/content (render-api-error album))]
    (l/compose-pews
      [(l/element= :a)          (l/attr :href (str "/album/" (:id album)))]
      [(l/element= :img)        (l/attr :src (str (-> album :image :extralarge)))]
      [(l/class= :album-title)  (l/content (:title album))]
      [(l/class= :album-artist) (l/content (format-artists album))]
      [(l/class= :album-label)  (l/content (format-labels album))]
      [(l/class= :album-media)  (l/content (format-media album))]
      [(l/class= :owners)       (l/content (pluralize (get album :owners 0) "owner"))]
      [(l/class= :buttons)      (l/content (render-album-buttons album :small))])))

(let [none-html (static :none)]
  (defn render-album-thumbs [albums]
    (if (seq albums)
      (for [row (partition 2 albums)]
        (l/node :div :attrs {:class "row"}
                :content (map render-album-thumb row)))
      [none-html])))

(defn render-tracks-table [album]
  (flatten
    (for [medium (:media album)]
      [(l/node :tr
               :content [(l/node :th :attrs {:colspan "3"}
                                 :content (str (:title medium)
                                               (when (:title medium) " — ")
                                               (get format-map (:format medium))))])
       (for [track (:tracks medium)]
         (l/node :tr
                 :content [(l/node :td :attrs {:class "col-lg-1"}
                                   :content (:number track))
                           (l/node :td :content (:title track))
                           (l/node :td :attrs {:class "col-lg-2 text-right"}
                                   :content (if-let [l (:length track)]
                                              (format-ms l)
                                              "?:??"))]))])))

(defragment render-album (template :album)
  [{:keys [id title] :as album}]
  [(l/id= :buttons)      (l/content (render-album-buttons album))]
  [(l/element= :img)     (l/attr :src (str (-> album :image :extralarge)))]
  [(l/id= :album-title)  (l/content title)]
  [(l/id= :album-artist) (l/content (format-artists album))]
  [(l/id= :album-media)  (l/content (format-media album))]
  [(l/id= :album-label)  (l/content (format-labels album))]
  [(l/element= :table)   (l/content (render-tracks-table album))]
  [(l/id= :musicbrainz)  (l/attr :href (str "http://musicbrainz.org/release/" (:mbid album)))])

(let [heading (heading-search "Top Albums" "/albums/search")]
  (defn top-albums-page [page]
    (let [albums (album/get-top-albums page 6)
          pages (count-pages (album/count-albums) 6)]
      (layout
        (concat heading
                (render-album-thumbs albums)
                (pager "/albums?" page pages))
        :active :albums))))

(defn search-page [query page]
  (let [{:keys [albums pages]} (album/search query page 6)
        title (str "Album search: " query)]
    (layout
      (concat (heading-search title "/albums/search" query)
              (render-album-thumbs albums)
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
