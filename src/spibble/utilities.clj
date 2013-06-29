(ns spibble.utilities)

(defn safe-parse-long
  "Safely parses a long, returning default or nil for malformed input."
  ([n] (safe-parse-long n nil))
  ([n default]
   (try
     (if n
       (Long/parseLong n)
       default)
     (catch NumberFormatException _
       default))))

(defn image-map [image]
  "Converts the image element returned by Last.fm into a map of size to URL."
  (into {} (map #(vector (keyword (:size %)) (:text %)) image)))
