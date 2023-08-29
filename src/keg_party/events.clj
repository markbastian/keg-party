(ns keg-party.events
  (:require
   [keg-party.htmx-notifications :as htmx-notifications]
   [clojure.tools.logging :as log]
   [generic.client-api :as client-api]))

(defn create-tap-message! [{:keys [client-manager] :as context}
                           {:keys [username message-id message] :as m}]
  {:pre [username message-id message]}
  (let [clients (client-api/clients client-manager username)]
    (log/infof "Broadcasting to %s clients." (count clients))
    (htmx-notifications/broadcast-tapped-data context clients m)))

(defn delete-tap-message! [{:keys [client-manager]} message-id]
  (let [clients (client-api/clients client-manager)]
    (htmx-notifications/broadcast-delete-data clients message-id)))
