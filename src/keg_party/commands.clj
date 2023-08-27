(ns keg-party.commands
  (:require
   [keg-party.events :as events]
   [keg-party.migrations :as migrations]
   [clojure.tools.logging :as log]
   [generic.commands :as cmd]))

(defmethod cmd/dispatch-command :tap-message
  [{:keys [ds] :as context} {:keys [command username message] :as m}]
  {:pre [ds command username message]}
  (log/infof "Dispatching command: %s" command)
  (if-some [user-id (:user/id (migrations/user ds {:username username}))]
    (let [message-id (migrations/create-tap! ds {:user-id user-id :tap message})]
      (events/create-tap-message! context (assoc m :message-id message-id)))
    (log/warnf "No user: %s" username)))

(defmethod cmd/dispatch-command :delete-message
  [{:keys [ds] :as context} {:keys [command message-id]}]
  (log/infof "Dispatching command %s for %s" command message-id)
  (migrations/delete-tap! ds message-id)
  (events/delete-tap-message! context message-id))
