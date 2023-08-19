(ns keg-party.htmx-notifications
  (:require [keg-party.pages :as pages]
            [generic.client-api :as client-api]
            [hiccup.page :refer [html5]]))

(defmulti transform (fn [{:keys [accept]} {:keys [event]}] [accept event]))

(defmethod transform [:htmx :data-tapped] [_ {:keys [_ client-id message-id message]}]
  (html5
   (pages/notifications-pane
    {:hx-swap-oob "afterbegin"}
    (pages/code-block client-id message-id message))))

(defmethod transform [:htmx :data-deleted] [_ {:keys [message-id]}]
  (html5
   [:div {:id          (format "code-block-%s" message-id)
          :style       "opacity: 0; transition: opacity 1s linear;"
          :hx-swap-oob "delete"}]))

(defn broadcast-tapped-data [clients {:keys [client-id message-id message]}]
  (let [html (html5
              (pages/notifications-pane
               {:hx-swap-oob "afterbegin"}
               (pages/code-block client-id message-id message)))]
    (client-api/broadcast! clients (keys clients) html)))

(defn broadcast-delete-data [clients message-id]
  (let [html (html5
              [:div {:id          (format "code-block-%s" message-id)
                     :style       "opacity: 0; transition: opacity 1s linear;"
                     :hx-swap-oob "delete"}])]
    (client-api/broadcast! clients (keys clients) html)))
