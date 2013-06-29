(ns spibble.models.album
  (:require [spibble.utilities :refer [image-map]]
            [monger.collection :as mc]
            [me.raynes.least :as least]
            [spibble.models.login :refer [api-key]]))

(mc/ensure-index "albums" {:id 1})

(defn search [query page]
  (let [results (least/read "album.search" api-key {:album query :limit 4 :page page})]
    {:albums (map #(update-in % [:image] image-map)
                  (get-in results [:results :albummatches :album]))
     :pages (-> results :results :opensearch:totalResults
                Long/parseLong
                (/ 4) Math/ceil int)}))
