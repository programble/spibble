(ns spibble.utilities
  (:require [spibble.config :refer [api-key]]
            [spibble.cache :as cache]
            [me.raynes.least :as least]))

(def lastfm
  "TTL memoized function for making Last.fm API calls."
  (cache/memoize #(least/read %1 api-key %2)))

(defn parse-long [n]
  (when n
    (try
      (Long/parseLong n)
      (catch NumberFormatException _
        nil))))

(defn parse-pos-long [n]
  (when-let [parsed (parse-long n)]
    (when (pos? parsed)
      parsed)))

(defn count-pages [n per]
  (long (Math/ceil (/ n per))))

(defn image-from-lastfm [image]
  "Process image elements from Last.fm for sanity."
  (into {} (map #(vector (keyword (:size %)) (:text %)) image)))

(defn pluralize [n s]
  (str n " " s (when-not (= n 1) "s")))

(defn format-ms [ms]
  (let [quotrem (juxt quot rem)
        secs (quot ms 1000)
        [mins rsecs] (quotrem secs 60)
        [hours rmins] (quotrem mins 60)]
    (if (pos? hours)
      (format "%d:%02d:%02d" hours rmins rsecs)
      (format "%d:%02d" mins rsecs))))
