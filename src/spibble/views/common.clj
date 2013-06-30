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
  [contents active query]
  (if-let [user (session/get :user)]
    [(l/id= :user) (comp (l/attr :href (str "/user/" (:name user)))
                         (l/content (:name user)))
     (l/class= :logged-out) (l/remove)]
    [(l/class= :logged-in) (l/remove)])
  (when active
    [(l/id= active) (l/add-class "active")])
  (when query
    [(l/class= :search-query) (l/attr :value query)])
  (l/id= :contents) (l/content contents))

(let [html (l/parse (template :html))]
  (defn layout [content & [{:keys [title active query]}]]
    (l/document
      html
      (l/element= :head) (l/content (head title))
      (l/element= :body) (l/content (body content active query)))))

(defragment pager (template :pager)
  [base page pages]
  (if (<= page 1)
    [(l/class= :previous) (l/remove)]
    [(l/id= :previous) (l/attr :href (str base "p=" (dec page)))])
  (if (>= page pages)
    [(l/class= :next) (l/remove)]
    [(l/id= :next) (l/attr :href (str base "p=" (inc page)))]))
