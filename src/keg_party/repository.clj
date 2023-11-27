(ns keg-party.repository)

(defprotocol IUserStore
  (create-user! [_ {:keys [username email password] :as user}])
  (users [_])
  (user [_ {:keys [id username email]}]))

(defprotocol IChannelStore
  (create-channel! [_ {:keys [name] :as channel}])
  (channels [_])
  (channel [_ {:keys [id name]}]))

(defprotocol ITapStore
  (create-tap! [this tap])
  (taps [this])
  (tap [this {:keys [id]}])
  (delete-tap! [this id])
  (get-recent-user-taps [_ user] [_ user limit] [_ user limit cursor])
  (get-recent-channel-taps [_ user] [_ channel limit] [_ channel limit cursor])
  (delete-unstarred-taps! [_ username]))

(defprotocol IFavoriteStore
  (get-favorite [_ {:keys [username user-id tap-id]}])
  (favorite [_ {:keys [username user-id tap-id]}])
  (unfavorite [_ {:keys [username user-id tap-id]}]))

(defprotocol IChannelActions
  (set-user-channel! [_ user channel])
  (get-user-channel [_ user])
  (get-channel-users [_ channel])
  (delete-channel! [_ channel]))
