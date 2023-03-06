(ns keg-party.domain
  (:require [keg-party.htmx-notifications :as htmx-notifications]))

(defn create-tap-message! [{:keys [clients _conn]}
                           {:keys [client-id message-id stack message] :as m}]
  {:pre [client-id message-id stack message]}
  (htmx-notifications/broadcast-tapped-data clients nil m))

(defn delete-tap-message! [{:keys [clients _conn]} message-id]
  (htmx-notifications/broadcast-delete-data clients nil message-id))
