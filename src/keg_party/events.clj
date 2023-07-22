(ns keg-party.events
  (:require [keg-party.client-api :as client-api]
            [keg-party.htmx-notifications :as htmx-notifications]))

(defn create-tap-message! [{:keys [client-manager]}
                           {:keys [client-id message-id message] :as m}]
  {:pre [client-id message-id message]}
  (htmx-notifications/broadcast-tapped-data (client-api/clients client-manager) nil m))

(defn delete-tap-message! [{:keys [client-manager]} message-id]
  (htmx-notifications/broadcast-delete-data (client-api/clients client-manager) nil message-id))
