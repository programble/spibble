(ns spibble.models.album
  (:require [clojure.core.memoize :as memo]
            [spibble.config :refer [api-key]]
            [spibble.utilities :refer [image-map count-pages]]
            [monger.collection :as mc]
            [monger.query :as mq]
            [me.raynes.least :as least]))

(mc/ensure-index "albums" {:id 1})

(def lastfm
  "TTL memoized function for making Last.fm API calls."
  (memo/ttl #(least/read %1 api-key %2) :ttl/threshold 3600000))

(defn vector-fix [v] (if (sequential? v) v [v]))

(defn from-lastfm
  "Process album returned by Last.fm for sanity."
  [album]
  (letfn [(from-tracks [t]
            (map #(dissoc % :streamable :attr) (:track t)))]
    (-> album
      (update-in [:id] #(Long/parseLong %))
      (update-in [:image] image-map)
      (update-in [:tracks] from-tracks)
      (dissoc :streamable :wiki :toptags))))

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
        remote (:album (lastfm "album.getInfo" query))]
    (merge (from-lastfm remote) local)))

(defn get-album
  "Get local and remote data for an album."
  [id]
  (get-remote (get-local id)))

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
  (let [results (:results (lastfm "album.search" {:album query :limit per :page page}))
        albums (map from-lastfm (-> results :albummatches :album vector-fix))
        pages (-> results :opensearch:totalResults Long/parseLong (count-pages per))]
    ;; Create local data for every search result
    (doseq [album albums]
      (when-not (get-local (:id album))
        (add-local album)))
    {:albums albums :pages pages}))
