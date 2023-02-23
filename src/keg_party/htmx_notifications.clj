(ns keg-party.htmx-notifications
  (:require [keg-party.client-api :as client-api]
            [keg-party.pages :as chat-pages]
            [keg-party.utils :as u]
            [hiccup.page :refer [html5]]))

(defn broadcast-to-room [clients _db _room-name message]
  (let [html (html5
              (chat-pages/notifications-pane
               {:hx-swap-oob "afterbegin"}
               [:div
                [:span [:pre [:code.language-clojure message]]
                 [:button.btn.btn-primary.btn-sm
                  [:i.fa-solid.fa-copy
                   {:onclick (format
                              "navigator.clipboard.writeText(atob('%s'))"
                              (u/base64-encode message))}]]]
                [:hr]]))]
    (client-api/broadcast! clients (keys clients) html)))
