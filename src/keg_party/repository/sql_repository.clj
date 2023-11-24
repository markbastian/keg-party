(ns keg-party.repository.sql-repository
  (:require
   [keg-party.repository :as repository]
   [honey.sql :as hsql]
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as result-set]
   [next.jdbc.sql :as sql]))

(defrecord CommandSqlImpl [ds])

(extend-type CommandSqlImpl
  repository/IUserStore
  (create-user! [{:keys [ds]} {:keys [username email password] :as user}]
    {:pre [username email password]}
    (jdbc/with-transaction [tx ds]
      (if-some [existing (sql/get-by-id tx :user username :username nil)]
        existing
        (do
          (sql/insert! tx :user (select-keys user [:username :email :password :channel_id]))
          (sql/get-by-id tx :user username :username nil)))))
  (users [{:keys [ds]}]
    (sql/find-by-keys ds :user :all {:order-by [:user/username]}))
  (user [{:keys [ds]} {:keys [id username email]}]
    {:pre [(or id username email)]}
    (if id
      (sql/get-by-id ds :user id)
      (let [arg (cond-> {}
                  username
                  (assoc :username username)
                  email
                  (assoc :email email))]
        (first (sql/find-by-keys ds :user arg)))))
  repository/IChannelStore
  (create-channel! [{:keys [ds]} {:keys [name] :as channel}]
    (jdbc/with-transaction [tx ds]
      (if-some [existing (sql/get-by-id tx :channel name :name nil)]
        existing
        (do
          (sql/insert! tx :channel (select-keys channel [:name]))
          (sql/get-by-id tx :channel name :name nil)))))
  (channels [{:keys [ds]}]
    ;;TODO - grok order-by here
    (sql/find-by-keys ds :channel :all))
  (channel [{:keys [ds]} {channel-name :name :keys [id]}]
    {:pre [(or id channel-name)]}
    (if id
      (sql/get-by-id ds :channel id)
      (let [arg (cond-> {}
                  channel-name
                  (assoc :name channel-name))]
        (first (sql/find-by-keys ds :channel arg)))))
  repository/ITapStore
  (create-tap! [{:keys [ds]} {:keys [user-id channel-id tap]}]
    {:pre [user-id channel-id tap]}
    (jdbc/with-transaction [tx ds]
      (when-some [tap-id ((keyword "last_insert_rowid()")
                          (sql/insert! tx :tap {:user_id user-id :channel_id channel-id :tap tap}))]
        (sql/get-by-id tx :tap tap-id))))
  (taps [{:keys [ds]}]
    (sql/find-by-keys ds :tap :all))
  (tap [{:keys [ds]} {:keys [id]}]
    (sql/get-by-id ds :tap id))
  (delete-tap! [{:keys [ds]} id]
    (let [{:tap/keys [user_id id]} (sql/get-by-id ds :tap id)
          favorite (first (sql/find-by-keys ds :favorite {:user_id user_id :tap_id id}))]
      (jdbc/with-transaction [tx ds]
        (when favorite
          (sql/delete! tx :favorite {:user_id user_id :tap_id id}))
        (sql/delete! tx :tap {:id id}))))
  (get-recent-user-taps
    ([this user]
     (repository/get-recent-user-taps this user 10))
    ([{:keys [ds]} user limit]
     (jdbc/execute!
      ds
      (hsql/format
       {:select    [:*]
        :from      [[:tap :T]]
        :left-join [[:user :U] [:= :U.id :T.user_id]]
        :where     [:= :U.username user]
        :order-by  [[:created_at :desc]
                    [:id :desc]]
        :limit     limit})))
    ([{:keys [ds]} user limit cursor]
     (jdbc/execute!
      ds
      (hsql/format
       {:select    [:*]
        :from      [[:tap :T]]
        :left-join [[:user :U] [:= :U.id :T.user_id]]
        :where     [:and
                    [:= :U.username user]
                    [:> cursor :T.id]]
        :order-by  [[:created_at :desc]
                    [:id :desc]]
        :limit     limit}))))
  (get-recent-channel-taps
    ([this channel]
     (repository/get-recent-channel-taps this channel 10))
    ([{:keys [ds]} channel limit]
     (jdbc/execute!
      ds
      (hsql/format
       {:select    [:*]
        :from      [[:tap :T]]
        :left-join [[:channel :C] [:= :C.id :T.channel_id]]
        :where     [:= :C.name channel]
        :order-by  [[:created_at :desc]
                    [:id :desc]]
        :limit     limit})))
    ([{:keys [ds]} channel limit cursor]
     (jdbc/execute!
      ds
      (hsql/format
       {:select    [:*]
        :from      [[:tap :T]]
        :left-join [[:channel :C] [:= :C.id :T.channel_id]]
        :where     [:and
                    [:= :C.name channel]
                    [:> cursor :T.id]]
        :order-by  [[:created_at :desc]
                    [:id :desc]]
        :limit     limit}))))
  (delete-unstarred-taps! [{:keys [ds]} username]
    (jdbc/execute!
     ds
     (hsql/format
      {:with      [[[:unstarred]
                    {:select    [[:T.id]]
                     :from      [[:tap :T]]
                     :left-join [[:user :U] [:= :U.id :T.user_id]
                                 [:favorite :F] [:= :F.tap_id :T.id]]
                     :where     [:and
                                 [:= :U.username username]
                                 [:= :T.channel_id :U.channel_id]
                                 [:= :F.tap_id nil]]}]]
       :delete    []
       :from      [:tap]
       :where     [:in :id :unstarred]
       :returning :id})
     {:builder-fn result-set/as-unqualified-kebab-maps}))
  repository/IFavoriteStore
  (get-favorite [{:keys [ds] :as this} {:keys [username user-id tap-id]}]
    (let [res (cond
                user-id (sql/find-by-keys ds :favorite {:user_id user-id :tap_id tap-id})
                username (let [user-id (:user/id (repository/user this {:username username :id user-id}))]
                           (sql/find-by-keys ds :favorite {:user_id user-id :tap_id tap-id})))]
      (some? (seq res))))
  (favorite [{:keys [ds] :as this} {:keys [username user-id tap-id]}]
    (cond
      user-id (sql/insert! ds :favorite {:user_id user-id :tap_id tap-id})
      username (let [user-id (:user/id (repository/user this {:username username :id user-id}))]
                 (sql/insert! ds :favorite {:user_id user-id :tap_id tap-id}))))

  (unfavorite [{:keys [ds] :as this} {:keys [username user-id tap-id]}]
    (cond
      user-id (sql/delete! ds :favorite {:user_id user-id :tap_id tap-id})
      username (let [user-id (:user/id (repository/user this {:username username :id user-id}))]
                 (sql/delete! ds :favorite {:user_id user-id :tap_id tap-id}))))
  repository/IChannelActions
  (set-user-channel! [{:keys [ds] :as this} user {channel-id :id :as channel}]
    (let [u          (zipmap (map (comp keyword name) (keys user)) (vals user))
          c          (zipmap (map (comp keyword name) (keys channel)) (vals channel))
          {user-channel-id :user/channel_id
           user-id         :user/id
           :as             user} (repository/user this u)
          channel-id (or
                      channel-id
                      (:channel/id (repository/channel this c)))]
      (if (= user-channel-id channel-id)
        user
        (jdbc/with-transaction [tx ds]
          (sql/update! tx :user {:channel_id channel-id} {:id user-id})
          (sql/get-by-id tx :user user-id)))))
  (get-user-channel [this user]
    (let [u          (zipmap (map (comp keyword name) (keys user)) (vals user))
          channel-id (:user/channel_id (repository/user this u))]
      (repository/channel this {:id channel-id})))
  (get-channel-users [{:keys [ds] :as this} channel]
    (let [c          (zipmap (map (comp keyword name) (keys channel)) (vals channel))
          channel-id (:channel/id (repository/channel this c))]
      (sql/find-by-keys ds :user {:channel_id channel-id} {:order-by [:user/username]}))))

(defn instance [ds]
  (->CommandSqlImpl ds))

(comment
  (require '[keg-party.system :as system])
  (system/restart!)
  (def ds (:parts.next.jdbc.core/datasource (system/system)))

  (let [repo (:keg-party.system/repository (system/system))]
    (sql/find-by-keys (:ds repo) :user {:username "mbastian"})
    (sql/find-by-keys (:ds repo) :user {:channel_id 1})
    ;(repository/users repo)
    )

  (let [repo (:keg-party.system/repository (system/system))]
    (repository/users repo))

  (let [repo (:keg-party.system/repository (system/system))]
    (repository/user repo {:id 2}))

  (let [impl (->CommandSqlImpl ds)]
    (repository/users impl)))
