(ns keg-party.domain-api)

(defprotocol IUserStore
  (create-user! [_ {:keys [username email password] :as user}])
  (users [_])
  (user [_ {:keys [id username email]}]))

(defprotocol ITapStore
  (create-tap! [_ tap])
  (taps [_])
  (tap [_ _])
  (delete-tap! [_ _])
  (get-recent-taps [_ _] [_ _ _] [_ _ _ _])
  (delete-unstarred-taps! [_ _]))

(defprotocol IFavoriteStore
  (get-favorite [_ _])
  (favorite [_ _])
  (unfavorite [_ _]))
