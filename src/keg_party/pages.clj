(ns keg-party.pages
  (:require [keg-party.utils :as u]
            [hiccup.page :refer [html5 include-css include-js]]))

(defn wrap-as-page [content]
  (html5
   (include-css
    "https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css"
    "https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.2.1/css/all.min.css"
    "https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.7.0/styles/intellij-light.min.css"
    ;"//cdnjs.cloudflare.com/ajax/libs/highlight.js/11.7.0/styles/default.min.css"
    )
   (include-js
    "https://unpkg.com/htmx.org@1.8.4"
    "https://unpkg.com/htmx.org/dist/ext/ws.js"
    "https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/js/bootstrap.bundle.min.js"
    "public/keg_party/highlight.min.js")
   ;[:script (slurp (io/resource "keg_party/highlight.min.js"))]
   [:script "hljs.highlightAll();"]
   [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
   content))

(defn notifications-pane [& r]
  (into [:div#notifications] r))

;; TODO: Use message uuid to assign ids to code blocks.
(defn code-block [client-id message-id message stack]
  (println message-id)
  (let [id (format "code-block-%s" message-id)]
    [:div
     {:id id}
     [:p client-id]
     [:div.d-flex.justify-content-between.align-items-center
      [:div
       [:pre
        [:code.language-clojure message]]]
      [:div.d-flex.flex-column.gap-1
       [:button.btn.btn-dark.btn-sm
        {:onclick (format
                   "navigator.clipboard.writeText(atob('%s'))"
                   (u/base64-encode message))}
        [:i.fa-solid.fa-copy]]
       [:button.btn.btn-dark.btn-sm
        {:ws-send "true"
         :hx-vals (u/to-json-str {:command    :delete-message
                                  :message-id message-id})
         :name    "delete-message"}
        [:i.fa-solid.fa-trash]]
       [:button.btn.btn-dark.btn-sm
        {:onclick (format
                   "navigator.clipboard.writeText(atob('%s'))"
                   (u/base64-encode stack))}
        [:i.fa-solid.fa-list]]]]
     [:hr]
     [:script "hljs.highlightAll();"]]))

(def chat-pane
  [:div#chat.overflow-scroll
   [:div.p-2
    (notifications-pane)]])

(defn navbar []
  [:nav.navbar.navbar-expand-lg.navbar-dark.bg-dark.sticky-top
   [:a.navbar-brand.mx-auto {:href "#"}
    [:img {:src "public/keg_party/rootbeer-sm.png" :width "30" :height "30" :alt ""}]]])

(defn chat-page [{:keys [params] :as _request}]
  (let [{:keys [client-id]} params]
    (html5
     [:div
      {:hx-ext     "ws"
       :ws-connect (format "/ws/%s" (or client-id (random-uuid)))}
      (navbar)
      chat-pane
      [:form
       {:ws-send "true"
        :name    "chat-message"
        :method  :post}]])))

(defn landing-page [request]
  (wrap-as-page (chat-page request)))
