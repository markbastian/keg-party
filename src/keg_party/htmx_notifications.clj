(ns keg-party.htmx-notifications
  (:require [keg-party.client-api :as client-api]
            [keg-party.pages :as chat-pages]
            [hiccup.page :refer [html5]]))

(defn broadcast-tapped-data [clients _db
                             {:keys [client-id message-id stack message]}]
  (let [html (html5
              (chat-pages/notifications-pane
               {:hx-swap-oob "afterbegin"}
               (chat-pages/code-block client-id message-id message stack)))]
    (client-api/broadcast! clients (keys clients) html)))

(defn broadcast-delete-data [clients _db message-id]
  (let [html (html5
              [:div {:id          (format "code-block-%s" message-id)
                     :hx-swap-oob "true"}])]
    (client-api/broadcast! clients (keys clients) html)))
