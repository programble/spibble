(ns spibble.models.album
  (:require [spibble.utilities :refer [lastfm count-pages image-from-lastfm]]
            [monger.collection :as mc]
            [monger.query :as mq]))

(mc/ensure-index "albums" {:id 1})

(defn vector-fix [v] (if (sequential? v) v [v]))

(defn track-from-lastfm [track]
  (-> track
      (select-keys [:name :mbid :url])
      (assoc :duration (Long/parseLong (:duration track))
             :artist (-> track :artist :name))))

(defn from-lastfm [album]
  (-> album
      (select-keys [:name :artist :mbid :url])
      (assoc :id (Long/parseLong (:id album))
             :image (image-from-lastfm (:image album))
             :tracks (map track-from-lastfm (-> album :tracks :track vector-fix)))))

(defn count-albums []
  (mc/count "albums" {}))

(defn get-local
  "Get local album data."
  [id]
  (mc/find-one-as-map "albums" {:id id}))

(defn get-remote
  "Populate local data map with remote data."
  [{:keys [mbid name artist] :as local}]
  (let [query (if-not (empty? mbid)
                {:mbid mbid}
                {:album name :artist artist})
        resp (lastfm "album.getInfo" query)]
    (if (:error resp)
      resp
      (merge (from-lastfm (:album resp)) local))))

(defn get-album
  "Get local and remote data for an album."
  [id]
  (when-let [local (get-local id)]
    (get-remote local)))

(defn add-local
  "Add local album data for a Last.fm album."
  [remote]
  (mc/insert "albums" (select-keys remote [:id :mbid :artist :name])))

(defn get-top-albums
  [page per]
  (map get-remote (mq/with-collection "albums"
                    (mq/find {})
                    ;; TODO: Sort by popularity
                    (mq/sort {:id 1})
                    (mq/paginate :page page :per-page per))))

(defn search
  "Search Last.fm for albums."
  [query page per]
  (let [resp (lastfm "album.search" {:album query :limit per :page page})]
    (if (:error resp)
      {:albums [resp], :pages 0}
      (let [albums (map from-lastfm (-> resp :results :albummatches :album vector-fix))
            pages (-> resp :results :opensearch:totalResults Long/parseLong (count-pages per))]
        ;; Create local data for every search result
        (doseq [album albums]
          (when-not (get-local (:id album))
            (add-local album)))
        {:albums albums, :pages pages}))))
