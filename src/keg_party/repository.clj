(ns keg-party.repository)

(defprotocol IUserStore
  (create-user! [_ {:keys [username email password] :as user}])
  (users [_])
  (user [_ {:keys [id username email]}]))

(defprotocol ITapStore
  (create-tap! [this tap])
  (taps [this])
  (tap [this {:keys [id]}])
  (delete-tap! [this id])
  (get-recent-taps [_ user] [_ user limit] [_ user limit cursor])
  (delete-unstarred-taps! [_ username]))

(defprotocol IFavoriteStore
  (get-favorite [_ {:keys [username user-id tap-id]}])
  (favorite [_ {:keys [username user-id tap-id]}])
  (unfavorite [_ {:keys [username user-id tap-id]}]))
