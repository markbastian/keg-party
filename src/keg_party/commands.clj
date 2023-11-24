(ns keg-party.commands
  (:require
   [keg-party.events :as events]
   [keg-party.repository :as repository]
   [clojure.tools.logging :as log]
   [generic.commands :as cmd]))

(defmethod cmd/dispatch-command :user-joined
  [context {:keys [command username] :as msg}]
  (log/infof "Command![%s:%s]" command username)
  (events/update-channels-list-message! context msg))

(defmethod cmd/dispatch-command :user-left
  [context {:keys [command username] :as msg}]
  (log/infof "Command![%s:%s]" command username)
  (events/update-channels-list-message! context msg))

(defmethod cmd/dispatch-command :tap-message
  [{:keys [repo] :as context} {:keys [command username message]}]
  {:pre [repo command username message]}
  (log/infof "Command![%s:%s]" command username)
  (if-some [{user-id :user/id channel-id :user/channel_id} (repository/user repo {:username username})]
    (if-some [tap (repository/create-tap! repo {:user-id    user-id
                                                :channel-id channel-id
                                                :tap        message})]
      (events/create-tap-message! context tap)
      (log/warnf "Unable to create tap for message: %s" message))
    (log/warnf "No user: %s" username)))

(defmethod cmd/dispatch-command :delete-message
  [{:keys [repo] :as context} {:keys [command message-id]}]
  (log/infof "Dispatching command %s for %s" command message-id)
  (repository/delete-tap! repo message-id)
  (events/delete-tap-message! context message-id))

(defmethod cmd/dispatch-command :create-favorite-tap
  [{:keys [repo] :as context} {:keys [command username message-id]}]
  (log/infof "Dispatching command %s for %s" command message-id)
  (repository/favorite repo {:username username :tap-id message-id})
  ;; TODO - broadcast to the channel containing the tap
  (events/create-favorite-tap-message!
   context
   {:username username :tap-id message-id}))

(defmethod cmd/dispatch-command :delete-favorite-tap
  [{:keys [repo] :as context} {:keys [command username message-id]}]
  (log/infof "Dispatching command %s for %s" command message-id)
  (repository/unfavorite repo {:username username :tap-id message-id})
  ;; TODO - broadcast to the channel containing the tap
  (events/delete-favorite-tap-message!
   context
   {:username username :tap-id message-id}))

(defmethod cmd/dispatch-command :delete-unstarred-taps
  [{:keys [repo] :as context} {:keys [command username message-id]}]
  (log/infof "Dispatching command %s for %s" command message-id)
  (when (seq (repository/delete-unstarred-taps! repo username))
    ;(events/bulk-delete-tap-messages! context (map :id ids))
    (events/bulk-reset-tap-messages!
     context
     {:username username})))

(defmethod cmd/dispatch-command :change-channel
  [{:keys [repo] :as context} {:keys [command username channel-name]}]
  (log/infof "Dispatching command %s for %s to channel %s" command username channel-name)
  (let [current-channel-id   (:user/channel_id (repository/user repo {:username username}))
        current-channel-name (:channel/name (repository/channel repo {:id current-channel-id}))]
    (if-not (= channel-name current-channel-name)
      (if-some [channel-id (:channel/id (repository/channel repo {:name channel-name}))]
        (do
          (log/infof "Moving %s from %s to %s" username current-channel-name channel-name)
          (repository/set-user-channel! repo {:username username} {:id channel-id})
          (events/update-channels-list-message! context nil)
          (events/bulk-reset-tap-messages! context {:username username}))
        (if-some [channel-id (:channel/id (repository/create-channel! repo {:name channel-name}))]
          (do
            (repository/set-user-channel! repo {:username username} {:id channel-id})
            (events/update-channels-list-message! context nil)
            (events/bulk-reset-tap-messages! context {:username username}))
          (log/warnf "Unable to create channel %s" channel-name)))
      (log/infof "User %s is already in channel %s" username channel-name))))
