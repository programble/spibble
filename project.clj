(defproject spibble "0.1.0-SNAPSHOT"
  :description "Scrobble records as you spin them"
  :url "http://spibble.programble.me"
  :license {:name "ISC License"
            :url "http://www.isc.org/software/license"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [me.raynes/least "0.1.4"]
                 [compojure "1.1.5"]
                 [lib-noir "0.5.6"]
                 [com.novemberain/monger "1.5.0-beta1"]
                 [me.raynes/laser "2.0.0-SNAPSHOT"]
                 [clj-config "0.2.0"]]
  :plugins [[lein-ring "0.8.2"]]
  :ring {:handler spibble.server/handler})
