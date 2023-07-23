(ns keg-party.commands
  (:require [keg-party.events :as events]
            [clojure.tools.logging :as log]
            [generic.commands :as cmd]))

(defmethod cmd/dispatch-command :tap-message
  [context {:keys [command client-id message-id message] :as m}]
  {:pre [client-id message-id message]}
  (log/infof "Dispatching command: %s" command)
  (events/create-tap-message! context m))

(defmethod cmd/dispatch-command :delete-message
  [context {:keys [command message-id]}]
  (log/infof "Dispatching command: %s" command)
  (events/delete-tap-message! context message-id))
