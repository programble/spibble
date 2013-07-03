(ns spibble.cache
  (:refer-clojure :exclude [memoize])
  (:require [monger.collection :as mc]))

(mc/ensure-index "cache" {:created 1} {:expireAfterSeconds 3600})
(mc/ensure-index "cache" {:hash 1})

;; This assumes that you'll never pass the same arguments to two different
;; memoized functions and expect different cached results.
(defn memoize [f]
  (fn [& args]
    (let [hash (hash args)
          cached (mc/find-one-as-map "cache" {:hash hash} [:value])]
      (if cached
        (:value cached)
        (let [value (apply f args)]
          (mc/insert "cache" {:hash hash
                              :value value
                              :created (java.util.Date.)})
          value)))))
