(ns keg-party.htmx-notifications
  (:require [keg-party.client-api :as client-api]
            [keg-party.pages :as chat-pages]
            [hiccup.page :refer [html5]]))

(defn broadcast-tapped-data [clients _db client-id message]
  (let [html (html5
              (chat-pages/notifications-pane
               {:hx-swap-oob "afterbegin"}
               (chat-pages/code-block client-id message)))]
    (client-api/broadcast! clients (keys clients) html)))
