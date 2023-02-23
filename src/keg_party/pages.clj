(ns keg-party.pages
  (:require    [clojure.java.io :as io]
               [hiccup.page :refer [html5 include-css include-js]]))

(defn wrap-as-page [content]
  (html5
   (include-css
    "https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css"
    "https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.2.1/css/all.min.css")
   (include-js
    "https://unpkg.com/htmx.org@1.8.4"
    "https://unpkg.com/htmx.org/dist/ext/ws.js"
    "https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/js/bootstrap.bundle.min.js")
   [:script (slurp (io/resource "keg_party/highlight.min.js"))]
   [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
   content))

(defn notifications-pane [& r]
  (into [:div#notifications] r))

(def chat-pane
  [:div#chat.overflow-scroll
   [:div.p-2
    (notifications-pane)]])

(defn chat-page [{:keys [params] :as _request}]
  (let [{:keys [username]} params]
    (html5
     [:div
      {:hx-ext     "ws"
       :ws-connect (format "/ws/%s" (or username (random-uuid)))}
      chat-pane
      [:form
       {:ws-send "true"
        :name    "chat-message"
        :method  :post}]])))

(defn landing-page [request]
  (wrap-as-page (chat-page request)))
