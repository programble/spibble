(ns spibble.models.album
  (:refer-clojure :exclude [sort find])
  (:require [spibble.utilities :refer [image-map]]
            [spibble.config :refer [api-key]]
            [monger.collection :as mc]
            [monger.query :refer [with-collection find sort limit paginate]]
            [me.raynes.least :as least]))

(mc/ensure-index "albums" {:id 1})

(defn album-map [album]
  (let [tracks (map #(dissoc % :streamable :attr) (-> album :tracks :track))]
    (-> album
        (update-in [:id] #(Long/parseLong %))
        (update-in [:image] image-map)
        (assoc :tracks tracks)
        (dissoc :streamable :wiki :toptags))))

(defn get-album-bare [id]
  (mc/find-one-as-map "albums" {:id id}))

(defn populate-bare [bare]
  (let [query (if-let [mbid (:mbid bare)]
                {:mbid mbid}
                {:album (:name bare) :artist (:artist bare)})
        lfm (least/read "album.getInfo" api-key query)]
    (println lfm)
    (merge (album-map (:album lfm)) bare)))

(defn get-album [id]
  (populate-bare (get-album-bare id)))

(defn add-album-bare [album]
  (let [bare (select-keys album [:id :mbid :artist :name])]
    (mc/insert "albums" bare)))

(defn get-top-albums []
  (map populate-bare (with-collection "albums"
                       (find {})
                       (limit 6))))

(defn search [query page]
  (let [results (least/read "album.search" api-key {:album query :limit 6 :page page})
        albums (map album-map (get-in results [:results :albummatches :album]))
        pages (-> results :results :opensearch:totalResults
                  Long/parseLong (/ 6) Math/ceil int)]
    (doseq [album albums]
      (when-not (get-album-bare (:id album))
        (add-album-bare album)))
    {:albums albums :pages pages}))
