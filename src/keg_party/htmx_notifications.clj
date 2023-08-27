(ns keg-party.htmx-notifications
  (:require [keg-party.pages :as pages]
            [generic.client-api :as client-api]
            [hiccup.page :refer [html5]]))

(defn broadcast-tapped-data [clients {:keys [username message-id message]}]
  (let [html (html5
              (pages/notifications-pane
               {:hx-swap-oob "afterbegin"}
               (pages/code-block username message-id message)))]
    (client-api/broadcast! clients html)))

(defn broadcast-delete-data [clients message-id]
  (let [html (html5
              [:div {:id          (format "code-block-%s" message-id)
                     :style       "opacity: 0; transition: opacity 1s linear;"
                     :hx-swap-oob "delete"}])]
    (client-api/broadcast! clients html)))
