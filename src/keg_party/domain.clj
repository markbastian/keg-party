(ns keg-party.domain
  (:require [keg-party.htmx-notifications :as htmx-notifications]
            [keg-party.queries :as queries]))

(defn create-chat-message! [{:keys [clients conn]} username message]
  ;;Business logic
  (let [room-name (queries/current-room-name @conn username)]
    (htmx-notifications/broadcast-to-room clients @conn room-name message)))
