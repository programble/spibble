(ns spibble.utilities
  (:require [clojure.core.memoize :as memo]
            [spibble.config :refer [api-key]]
            [me.raynes.least :as least]))

(def lastfm
  "TTL memoized function for making Last.fm API calls."
  (memo/ttl #(least/read %1 api-key %2) :ttl/threshold 3600000))

;; From refheap.utilities
(defn safe-parse-long
  "Safely parse a long, returning default or nil for malformed input."
  ([n] (safe-parse-long n nil))
  ([n default]
   (try
     (if n
       (Long/parseLong n)
       default)
     (catch NumberFormatException _
       default))))

(defn count-pages [n per]
  (long (Math/ceil (/ n per))))

(defn image-from-lastfm [image]
  "Process image elements from Last.fm for sanity."
  (into {} (map #(vector (keyword (:size %)) (:text %)) image)))
