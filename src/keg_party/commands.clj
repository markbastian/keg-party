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

(defmethod cmd/dispatch-command :create-favorite-tap
  [{:keys [ds] :as context} {:keys [command username message-id]}]
  (log/infof "Dispatching command %s for %s" command message-id)
  (migrations/favorite ds {:username username :tap-id message-id})
  ;; TODO - At some point, we should broadcast this event, but
  ;; we really need shared taps first. ATM we only see our own
  ;; taps and the optimistic update is sufficient. Another failure
  ;; is if you favorite your tap in one window but have another
  ;; session open somewhere else you won't get that update until
  ;; this is live.
  (events/create-favorite-tap-message!
   context
   {:username username :tap-id message-id}))

(defmethod cmd/dispatch-command :delete-favorite-tap
  [{:keys [ds] :as context} {:keys [command username message-id]}]
  (log/infof "Dispatching command %s for %s" command message-id)
  (migrations/unfavorite ds {:username username :tap-id message-id})
  ;; TODO - At some point, we should broadcast this event, but
  ;; we really need shared taps first. ATM we only see our own
  ;; taps and the optimistic update is sufficient. Another failure
  ;; is if you favorite your tap in one window but have another
  ;; session open somewhere else you won't get that update until
  ;; this is live.
  (events/delete-favorite-tap-message!
   context
   {:username username :tap-id message-id}))

(defmethod cmd/dispatch-command :delete-unstarred-taps
  [{:keys [ds] :as context} {:keys [command username message-id]}]
  (log/infof "Dispatching command %s for %s" command message-id)
  (let [ids (migrations/delete-unstarred-taps! ds username)]
    (events/bulk-delete-tap-messages! context (map :id ids))))
