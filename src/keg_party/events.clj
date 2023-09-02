(ns keg-party.events
  (:require
   [keg-party.pages :as pages]
   [generic.client-api :as client-api]
   [hiccup.core :refer [html]]))

;; ATM events do the following rather than some fancy polymorphic async dispatch
;; system, but I don't want that level of complexity until we know we need it.
;; - Create event here
;; - Figure out who sees it based
;; - Generate the right format based on the client type (e.g. htmx, json, socket, etc.)
;;
;; Note that the client abstraction is generic, so we can ignore implementation.
;; However, we do expect the target format to be htmx, which is reasonable.
;; In some fancy world we could have some polymorphic dispatch on client protocol
;; (ws, http, etc.) and format (e.g. htmx, json, etc.) but for now let's keep it nice and simple.

(defn create-tap-message! [{:keys [client-manager] :as context}
                           {:keys [username message-id message]}]
  {:pre [username message-id message]}
  (let [clients (client-api/clients client-manager username)
        html    (html
                 (pages/notifications-pane
                  {:hx-swap-oob "afterbegin"}
                  (pages/code-block context username message-id message)))]
    (client-api/broadcast! clients html)))

(defn delete-tap-message! [{:keys [client-manager]} message-id]
  (let [clients (client-api/clients client-manager)
        html    (html
                 [:div {:id          (format "code-block-%s" message-id)
                        :style       "opacity: 0; transition: opacity 1s linear;"
                        :hx-swap-oob "delete"}])]
    (client-api/broadcast! clients html)))

(defn bulk-delete-tap-messages! [{:keys [client-manager]} message-ids]
  (let [clients (client-api/clients client-manager)
        html    (html
                 (for [message-id message-ids]
                   [:div {:id          (format "code-block-%s" message-id)
                          :hx-swap-oob "delete"}]))]
    (client-api/broadcast! clients html)))

(defn bulk-reset-tap-messages! [{:keys [client-manager] :as context} {:keys [username]}]
  (let [clients (client-api/clients client-manager username)
        html    (html
                 (pages/notifications-pane
                  {:hx-swap-oob "true"}
                  (pages/recent-taps context)))]
    (client-api/broadcast! clients html)))

(defn create-favorite-tap-message! [{:keys [client-manager]}
                                    {:keys [username tap-id]}]
  (let [clients (client-api/clients client-manager username)
        htmx    (html
                 (pages/favorite-star username tap-id {:hx-swap-oob "true"
                                                       :style       "color:#FFD700;"}))]
    (client-api/broadcast! clients htmx)))

(defn delete-favorite-tap-message! [{:keys [client-manager]}
                                    {:keys [username tap-id]}]
  (let [clients (client-api/clients client-manager username)
        htmx    (html
                 (pages/favorite-star username tap-id {:hx-swap-oob "true"}))]
    (client-api/broadcast! clients htmx)))
