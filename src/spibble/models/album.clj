(ns spibble.models.album
  (:require [spibble.utilities :refer [image-map]]
            [spibble.config :refer [api-key]]
            [monger.collection :as mc]
            [me.raynes.least :as least]))

(mc/ensure-index "albums" {:id 1})

(defn search [query page]
  (let [results (least/read "album.search" api-key {:album query :limit 6 :page page})]
    {:albums (map #(update-in % [:image] image-map)
                  (get-in results [:results :albummatches :album]))
     :pages (-> results :results :opensearch:totalResults
                Long/parseLong
                (/ 6) Math/ceil int)}))
