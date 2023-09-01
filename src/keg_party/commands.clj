(ns keg-party.commands
  (:require
   [keg-party.domain-api :as domain-api]
   [keg-party.events :as events]
   [clojure.tools.logging :as log]
   [generic.commands :as cmd]))

(defmethod cmd/dispatch-command :tap-message
  [{:keys [ds api] :as context} {:keys [command username message] :as m}]
  {:pre [ds command username message]}
  (log/infof "Dispatching command: %s" command)
  (if-some [user-id (:user/id (domain-api/user api {:username username}))]
    (let [message-id (domain-api/create-tap! api {:user-id user-id :tap message})]
      (events/create-tap-message! context (assoc m :message-id message-id)))
    (log/warnf "No user: %s" username)))

(defmethod cmd/dispatch-command :delete-message
  [{:keys [api] :as context} {:keys [command message-id]}]
  (log/infof "Dispatching command %s for %s" command message-id)
  (domain-api/delete-tap! api message-id)
  (events/delete-tap-message! context message-id))

(defmethod cmd/dispatch-command :create-favorite-tap
  [{:keys [api] :as context} {:keys [command username message-id]}]
  (log/infof "Dispatching command %s for %s" command message-id)
  (domain-api/favorite api {:username username :tap-id message-id})
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
  [{:keys [api] :as context} {:keys [command username message-id]}]
  (log/infof "Dispatching command %s for %s" command message-id)
  (domain-api/unfavorite api {:username username :tap-id message-id})
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
  [{:keys [api] :as context} {:keys [command username message-id]}]
  (log/infof "Dispatching command %s for %s" command message-id)
  (let [ids (domain-api/delete-unstarred-taps! api username)]
    (events/bulk-delete-tap-messages! context (map :id ids))))
