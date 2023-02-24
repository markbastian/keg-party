(ns keg-party.domain
  (:require [keg-party.htmx-notifications :as htmx-notifications]))

(defn create-tap-message! [{:keys [clients _conn]} client-id message]
  (htmx-notifications/broadcast-tapped-data clients nil client-id message))
