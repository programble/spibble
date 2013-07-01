(ns spibble.views.common
  (:require [clojure.java.io :refer [resource]]
            [me.raynes.laser :as l :refer [defragment]]
            [noir.session :as session]))

(defn template [file]
  (resource (str "spibble/views/templates/" (name file) ".html")))

(let [html (l/parse (template :layout))]
  (defn layout [content & {:keys [title active query]}]
    (l/document
      html
      (l/element= :title) (l/content (or title "Spibble"))

      (if-let [user (session/get :user)]
        [(l/id= :user) (comp (l/attr :href (str "/user/" (:name user)))
                             (l/content (:name user)))
         (l/class= :logged-out) (l/remove)]
        [(l/class= :logged-in) (l/remove)])

      (when active
        [(l/id= active) (l/add-class "active")])

      (when query
        [(l/class= :search-query) (l/attr :value query)])

      (l/id= :contents) (l/content content))))

(defragment pager (template :pager)
  [base page pages]
  (if (<= page 1)
    [(l/class= :previous) (l/remove)]
    [(l/id= :previous) (l/attr :href (str base "p=" (dec page)))])
  (if (>= page pages)
    [(l/class= :next) (l/remove)]
    [(l/id= :next) (l/attr :href (str base "p=" (inc page)))]))
