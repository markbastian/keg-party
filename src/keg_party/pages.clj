(ns keg-party.pages
  "Functions to creat server rendered pages."
  (:require [keg-party.utils :as u]
            [hiccup.page :refer [html5 include-css include-js]]))

(defn expand-collapse-block [show-collapse target-id]
  [(if show-collapse
     :i.fa-regular.fa-square-caret-up
     :i.fa-regular.fa-square-caret-down)
   {:type           "button"
    :data-bs-toggle "collapse"
    :data-bs-target target-id
    :hx-post        "/collapse"
    :hx-vals        (u/to-json-str {:show-collapse (not show-collapse)
                                    :target-id     target-id})
    :hx-swap        "outerHTML"}])

(defn code-block [client-id message-id message]
  (let [id (format "code-block-%s" message-id)]
    [:div
     {:id id}
     [:div.collapse.show
      {:id (format "%s-collapse" id)}
      [:p client-id]
      [:div.d-flex.justify-content-between.align-items-top
       [:div.overflow-auto
        [:pre
         [:code.language-clojure message]]
        [:script "hljs.highlightAll();"]]
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
         [:i.fa-solid.fa-trash]]]]]
     [:div.row.align-items-center
      [:div.col [:hr]]
      [:div.col-auto
       (expand-collapse-block true (format "#%s-collapse" id))]
      [:div.col [:hr]]]]))

(defn notifications-pane [& r]
  (into [:div#tap-log.p-2] r))

(defn navbar []
  [:nav.navbar.navbar-expand-lg.navbar-dark.bg-dark.sticky-top
   [:a.navbar-brand.mx-auto {:href "#"}
    [:img {:src "public/keg_party/rootbeer-sm.png" :width "30" :height "30" :alt ""}]]])

(defn landing-page [{:keys [params] :as _request}]
  (let [{:keys [client-id]} params]
    [:div
     {:hx-ext     "ws"
      :ws-connect (format "/ws/%s" (or client-id (random-uuid)))}
     (navbar)
     (notifications-pane)
     [:form
      {:ws-send "true"
       :name    "chat-message"
       :method  :post}]]))

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

(defn landing-page-html [request]
  (wrap-as-page (landing-page request)))
