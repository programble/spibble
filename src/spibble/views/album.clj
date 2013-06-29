(ns spibble.views.album
  (:require [spibble.models.album :as album]
            [spibble.views.common :refer [layout template]]
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

(defragment album-search (template :search)
  [query albums page page-max]
  (l/class= :thumbnails) (l/content (album-thumbs albums))
  (if (= page 1)
    [(l/class= :previous) (l/attr :class "previous disabled")]
    [(l/id= :previous) (l/attr :href (str "/search?q=" query "&p=" (dec page)))])
  (if (= page page-max)
    [(l/class= :next) (l/attr :class "next disabled")]
    [(l/id= :next) (l/attr :href (str "/search?q=" query "&p=" (inc page)))]))

(defn albums-page []
  (layout
    (l/unescaped "<p>TODO: List popular albums</p>")))

(defn search-page [query page]
  (layout
    (album-search query (album/search query page) page 3)
    (str "Album search: " query)))

(defroutes album-routes
  (GET "/" []
    (if (session/get :user)
      (redirect "/library")
      (albums-page)))

  (GET "/albums" []
    (albums-page))

  (GET "/search" [q p]
    (search-page q (safe-parse-long p 1))))
