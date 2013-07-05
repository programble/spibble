(ns spibble.models.login
  (:require [spibble.models.user :as user]
            [spibble.config :refer [api-key api-secret]]
            [me.raynes.least.authorize :as auth]))

(defn login [token]
  (when-let [session (auth/get-session api-key api-secret token)]
    (or (user/get-user (:name session))
        (user/get-remote (user/add-user session)))))
