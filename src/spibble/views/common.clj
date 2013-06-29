(ns spibble.views.common
  (:require [noir.session :as session]
            [me.raynes.laser :refer [defragment] :as l]
            [clojure.java.io :refer [resource]]))

(defn template [file]
  (resource (str "spibble/views/templates/" (name file) ".html")))

(defragment head (template :head)
  [title]
  (l/element= :title) (l/content (or title "Spibble")))

(defragment body (template :body)
  [contents]
  (if-let [user (session/get :user)]
    [(l/id= "user-name") (comp (l/attr :href (str "/user/" (:name user)))
                               (l/content (:name user)))
     (l/id= "login") (l/remove)]
    [(l/id= "user") (l/remove)
     (l/id= "logout") (l/remove)])
  (l/id= :contents) (l/content contents))

(let [html (l/parse (template :html))]
  (defn layout [content & [title]]
    (l/document
      html
      (l/element= :head) (l/content (head title))
      (l/element= :body) (l/content (body content)))))
