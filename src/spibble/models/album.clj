(ns spibble.models.album
  (:require [spibble.utilities :refer [count-pages]]
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

(defn refresh-data [album]
  (if (:refresh album)
    (let [data (mb/lookup :release
                          (:mbid album)
                          [:artists :labels :recordings :release-groups :media])]
      (if (:error data)
        data
        (mc/find-and-modify "albums"
                            {:id (:id album)}
                            (merge (dissoc album :refresh) (from-mb data))
                            :return-new true)))
    album))

(defn count-albums []
  (mc/count "albums" {}))

(defn get-album [id]
  (refresh-data (mc/find-one-as-map "albums" {:id id})))

(defn get-album-by-mbid [mbid]
  (refresh-data (mc/find-one-as-map "albums" {:mbid mbid})))

(defn get-top-albums [page per]
  (map refresh-data (mq/with-collection "albums"
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
            albums (map (comp get-or-add from-mb) (:releases resp))]
        {:albums albums, :pages pages}))))
