(ns spibble.config
  (:require [clj-config.core :as cfg]))

(def config (cfg/read-config "config.clj"))

(def api-key (:api-key config))
(def api-secret (:api-secret config))
