(ns keg-party.domain
  (:require [keg-party.htmx-notifications :as htmx-notifications]))

(defn create-tap-message! [{:keys [clients _conn]} client-id message-id message]
  (htmx-notifications/broadcast-tapped-data clients nil client-id message-id message))

(defn delete-tap-message! [{:keys [clients _conn]} message-id]
  (htmx-notifications/broadcast-delete-data clients nil message-id))
