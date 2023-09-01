(ns keg-party.domain-sql-impl
  (:require
   [keg-party.domain-api :as api]
   [honey.sql :as hsql]
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as result-set]
   [next.jdbc.sql :as sql]))

(defrecord CommandSqlImpl [ds])

(extend-type CommandSqlImpl
  api/IUserStore
  (create-user! [{:keys [ds]} {:keys [username email password] :as user}]
    {:pre [username email password]}
    (sql/insert! ds :user (select-keys user [:username :email :password])))
  (users [{:keys [ds]}]
    (sql/find-by-keys ds :user :all))
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
  api/ITapStore
  (create-tap! [{:keys [ds]} {:keys [user-id tap]}]
    {:pre [user-id tap]}
    ((keyword "last_insert_rowid()")
     (sql/insert! ds :tap {:user_id user-id :tap tap})))
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
  (get-recent-taps
    ([this user]
     (api/get-recent-taps this user 10))
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
                                 [:= :F.tap_id nil]]}]]
       :delete    []
       :from      [:tap]
       :where     [:in :id :unstarred]
       :returning :id})
     {:builder-fn result-set/as-unqualified-kebab-maps}))
  api/IFavoriteStore
  (get-favorite [{:keys [ds] :as this} {:keys [username user-id tap-id]}]
    (let [res (cond
                user-id (sql/find-by-keys ds :favorite {:user_id user-id :tap_id tap-id})
                username (let [user-id (:user/id (api/user this {:username username :id user-id}))]
                           (sql/find-by-keys ds :favorite {:user_id user-id :tap_id tap-id})))]
      (some? (seq res))))
  (favorite [{:keys [ds] :as this} {:keys [username user-id tap-id]}]
    (cond
      user-id (sql/insert! ds :favorite {:user_id user-id :tap_id tap-id})
      username (let [user-id (:user/id (api/user this {:username username :id user-id}))]
                 (sql/insert! ds :favorite {:user_id user-id :tap_id tap-id}))))

  (unfavorite [{:keys [ds] :as this} {:keys [username user-id tap-id]}]
    (cond
      user-id (sql/delete! ds :favorite {:user_id user-id :tap_id tap-id})
      username (let [user-id (:user/id (api/user this {:username username :id user-id}))]
                 (sql/delete! ds :favorite {:user_id user-id :tap_id tap-id})))))

(defn instance [ds]
  (->CommandSqlImpl ds))

(comment
  (require '[keg-party.system :as system])
  (def ds (:parts.next.jdbc.core/datasource (system/system)))

  (let [impl (->CommandSqlImpl ds)]
    (api/users impl)))
