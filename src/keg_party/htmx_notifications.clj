(ns keg-party.htmx-notifications
  (:require [keg-party.client-api :as client-api]
            [keg-party.pages :as pages]
            [hiccup.page :refer [html5]]))

(defn broadcast-tapped-data [clients _db
                             {:keys [client-id message-id message]}]
  (let [html (html5
              (pages/notifications-pane
               {:hx-swap-oob "afterbegin"}
               (pages/code-block client-id message-id message)))]
    (client-api/broadcast! clients (keys clients) html)))

(defn broadcast-delete-data [clients _db message-id]
  (let [html (html5
              [:div {:id          (format "code-block-%s" message-id)
                     :style "opacity: 0; transition: opacity 1s linear;"
                     :hx-swap-oob "delete"
                     ;NOPE
                     ;:hx-swap "outerHTML swap:1s"
                     }])]
    (client-api/broadcast! clients (keys clients) html)))
