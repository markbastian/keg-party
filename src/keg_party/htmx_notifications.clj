(ns keg-party.htmx-notifications
  (:require [keg-party.pages :as pages]
            [generic.client-api :as client-api]
            [hiccup.page :refer [html5]]))

(defn broadcast-tapped-data [context clients {:keys [username message-id message]}]
  ;; NOTE: When we start sharing taps this will require a change since favoriting
  ;; is user-specific so each code block may have a different star color depending
  ;; on who the user is.
  (let [html (html5
              (pages/notifications-pane
               {:hx-swap-oob "afterbegin"}
               (pages/code-block context username message-id message)))]
    (client-api/broadcast! clients html)))

(defn broadcast-delete-data [clients message-id]
  (let [html (html5
              [:div {:id          (format "code-block-%s" message-id)
                     :style       "opacity: 0; transition: opacity 1s linear;"
                     :hx-swap-oob "delete"}])]
    (client-api/broadcast! clients html)))
