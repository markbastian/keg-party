(ns keg-party.events
  (:require [keg-party.htmx-notifications :as htmx-notifications]))

(defn create-tap-message! [{:keys [clients]}
                           {:keys [client-id message-id message] :as m}]
  {:pre [client-id message-id message]}
  (htmx-notifications/broadcast-tapped-data clients nil m))

(defn delete-tap-message! [{:keys [clients]} message-id]
  (htmx-notifications/broadcast-delete-data clients nil message-id))
