(ns spibble.models.album
  (:require [clojure.core.memoize :as memo]
            [spibble.config :refer [api-key]]
            [spibble.utilities :refer [image-map]]
            [monger.collection :as mc]
            [monger.query :as mq]
            [me.raynes.least :as least]))

(def lastfm
  "TTL memoized function for making Last.fm API calls."
  (memo/ttl #(least/read %1 api-key %2) :ttl/threshold 3600000))

(mc/ensure-index "albums" {:id 1})

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
  "Get top 6 popular albums."
  []
  (map get-remote (mq/with-collection "albums"
                    (mq/find {})
                    ;; TODO: Sort by popularity
                    (mq/sort {:id 1})
                    (mq/limit 6))))

(defn search
  "Search Last.fm for albums in pages of 6."
  [query page]
  (let [results (:results (lastfm "album.search" {:album query :limit 6 :page page}))
        albums (map from-lastfm (-> results :albummatches :album))
        pages (-> results :opensearch:totalResults Long/parseLong (/ 6) Math/ceil int)]
    ;; Create local data for every search result
    (doseq [album albums]
      (when-not (get-local (:id album))
        (add-local album)))
    {:albums albums :pages pages}))
