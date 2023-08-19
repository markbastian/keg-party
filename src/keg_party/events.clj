(ns keg-party.events
  (:require [keg-party.htmx-notifications :as htmx-notifications]
            [generic.client-api :as client-api]))

(defn create-tap-message! [{:keys [client-manager]}
                           {:keys [client-id message-id message] :as m}]
  {:pre [client-id message-id message]}
  ;(let [clients (client-api/clients client-manager)]
  ;  (client-api/broadcast! clients (keys clients) 'html))
  (htmx-notifications/broadcast-tapped-data (client-api/clients client-manager) m))

(defn delete-tap-message! [{:keys [client-manager]} message-id]
  (htmx-notifications/broadcast-delete-data (client-api/clients client-manager) message-id))
