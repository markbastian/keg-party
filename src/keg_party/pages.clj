(ns keg-party.pages
  "Functions to create server rendered pages."
  (:require
   [keg-party.pages.clients :as clients]
   [keg-party.pages.detail :as detail]
   [keg-party.pages.feed :as feed]
   [keg-party.pages.login :as login]
   [keg-party.pages.signup :as signup]
   [hiccup.page :refer [html5 include-css include-js]]))

(defn- wrap-as-page [content]
  (html5
   [:head
    [:base {:href "/"}]
    (include-css
     "public/keg_party/css/bootstrap/5.2.3/bootstrap.min.css"
     "https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.2.1/css/all.min.css"
     ;"public/keg_party/css/font-awesome/6.2.1/all.min.css"
     "public/keg_party/css/highlight.js/11.7.0/styles/intellij-light.min.css"
     "public/keg_party/css/keg-party.css")
    (include-js
     "public/keg_party/js/htmx/1.9.5/htmx.min.js"
     "public/keg_party/js/htmx/1.9.5/ext/ws.js"
     "public/keg_party/js/bootstrap/5.2.3/bootstrap.bundle.min.js"
     "public/keg_party/js/highlight.js/highlight.min.js"
     "public/keg_party/js/keg-party.js")
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]]
   content))

;; Top level pages

(defn signup-page [request]
  (wrap-as-page
   (signup/signup-page request)))

(defn login-page [request]
  (wrap-as-page
   (login/login-page request)))

(defn feed-page [request]
  (wrap-as-page
   (feed/feed-page request)))

(defn tap-detail-page [request tap drill-path]
  (wrap-as-page
   (detail/tap-detail-page
    request tap drill-path)))

(defn clients-page [request]
  (wrap-as-page
   (clients/clients-page request)))
