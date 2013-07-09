(ns spibble.models.album
  (:require [spibble.utilities :refer [lastfm image-from-lastfm count-pages]]
            [spibble.cache :as cache]
            [cljmb.core :as mb]
            [monger.collection :as mc]
            [monger.operators :refer [$all]]
            [monger.query :as mq]
            [clojure.string :refer [escape]]))

(mc/ensure-index "albums" {:id 1})
(mc/ensure-index "albums" {:mbid 1})

(def album-id
  (atom (-> (mq/with-collection "albums"
              (mq/find {})
              (mq/sort {:id -1})
              (mq/limit 1))
            first :id (or 0))))

(def mb-search (cache/memoize mb/search))

(defn from-mb [release]
  (dissoc (assoc release :mbid (:id release)) :id))

(defn image-data [album]
  (let [artist (-> album :artist-credit first :artist :name)
        resp (lastfm "album.getInfo" {:artist artist, :album (:title album)})]
    {:image (-> resp :album :image image-from-lastfm)}))

(defn mb-data [album]
  (let [data (mb/lookup :release (:mbid album) [:artists
                                                :labels
                                                :media
                                                :recordings
                                                :release-groups])]
    (from-mb data)))

(defn refresh-album [album]
  (if (:refresh album)
    (let [refresh (merge album (mb-data album) (image-data album))]
      (mc/update "albums" {:id (:id album)} refresh)
      refresh)
    album))

(defn count-albums []
  (mc/count "albums" {}))

(defn get-album [id]
  (refresh-album (mc/find-one-as-map "albums" {:id id})))

(defn get-album-by-mbid [mbid]
  (refresh-album (mc/find-one-as-map "albums" {:mbid mbid})))

(defn get-top-albums [page per]
  (map refresh-album (mq/with-collection "albums"
                       (mq/find {})
                       (mq/sort {:owners -1})
                       (mq/paginate :page page :per-page per))))

(defn vinyl-query [query]
  (str \" (escape query {\" "\\\""}) "\" AND ("
       "format:\"12\\\" Vinyl\" OR "
       "format:\"10\\\" Vinyl\" OR "
       "format:\"7\\\" Vinyl\")"))

(defn get-or-add [release]
  (if-let [local (get-album-by-mbid (:mbid release))]
    local
    (let [album (assoc release :id (swap! album-id inc), :refresh true)]
      (mc/insert-and-return "albums" album))))

(defn search [query page per]
  (let [resp (mb-search :release (vinyl-query query) per page)]
    (if (:error resp)
      {:albums [resp], :pages 0}
      (let [pages (count-pages (:count resp) per)
            albums (for [release (:releases resp)]
                     (let [album (get-or-add (from-mb release))]
                       (if (:image album)
                         album
                         (merge album (image-data album)))))]
        {:albums albums, :pages pages}))))
