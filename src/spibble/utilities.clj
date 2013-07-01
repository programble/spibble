(ns spibble.utilities)

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
