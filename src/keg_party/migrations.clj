(ns keg-party.migrations
  (:require
   [honey.sql :as hsql]
   [next.jdbc :as jdbc]
   [next.jdbc.sql :as sql]))

(def create-user-table-ddl
  (hsql/format
   {:create-table [:user :if-not-exists]
    :with-columns
    [[:id :integer :primary-key]
     [:username :text [:not nil] :unique]
     [:email :text [:not nil] :unique]
     [:password :text [:not nil]]
     [:created_at :timestamp [:not nil] [:default :current_timestamp]]]}))

(def create-tap-table-ddl
  (hsql/format
   {:create-table [:tap :if-not-exists]
    :with-columns
    [[:id :integer :primary-key]
     [:user_id :int [:not nil]]
     [:created_at :timestamp [:not nil] [:default :current_timestamp]]
     [:tap :text [:not nil]]
     [[:foreign-key :user_id] [:references :user :id]]]}))

(def create-favorite-table-ddl
  (hsql/format
   {:create-table [:favorite :if-not-exists]
    :with-columns
    [[:user_id :int [:not nil]]
     [:tap_id :int [:not nil]]
     [[:foreign-key :user_id] [:references :user :id]]
     [[:foreign-key :tap_id] [:references :tap :id]]
     [[:primary-key :user_id :tap_id]]]}))

(def migrations
  [create-user-table-ddl
   create-tap-table-ddl
   create-favorite-table-ddl])

(defn create-user! [ds {:keys [username email password] :as user}]
  {:pre [username email password]}
  (sql/insert! ds :user (select-keys user [:username :email :password])))

(defn read-users [ds]
  (sql/find-by-keys ds :user :all))

(defn user [ds {:keys [id username email]}]
  {:pre [(or id username email)]}
  (if id
    (sql/get-by-id ds :user id)
    (let [arg (cond-> {}
                username
                (assoc :username username)
                email
                (assoc :email email))]
      (first (sql/find-by-keys ds :user arg)))))

(defn read-user [ds id]
  (sql/get-by-id ds :user id))

(defn read-user-by-username [ds username]
  (first (sql/find-by-keys ds :user {:username username})))

(defn read-user-by-email [ds email]
  (first (sql/find-by-keys ds :user {:email email})))

(defn create-tap! [ds {:keys [user-id tap]}]
  {:pre [user-id tap]}
  ((keyword "last_insert_rowid()") (sql/insert! ds :tap {:user_id user-id :tap tap})))

(defn read-taps [ds]
  (sql/find-by-keys ds :tap :all))

(defn read-tap [ds id]
  (sql/get-by-id ds :tap id))

(defn delete-tap! [ds id]
  (sql/delete! ds :tap {:id id}))

(defn favorite [ds user-id tap-id]
  (sql/insert! ds :favorites {:user_id user-id :tap_id tap-id}))

(defn unfavorite [ds user-id tap-id]
  (sql/delete! ds :favorites {:user_id user-id :tap_id tap-id}))

(defn get-recent-taps
  ([ds user]
   (get-recent-taps ds user 10))
  ([ds user limit]
   (jdbc/execute!
    ds
    (hsql/format
     {:select    [:*]
      :from      [[:tap :T]]
      :left-join [[:user :U] [:= :U.id :T.user_id]]
      :where     [:= :U.username user]
      :order-by  [[:created_at :desc]]
      :limit     limit})))
  ([ds user limit cursor]
   (jdbc/execute!
    ds
    (hsql/format
     {:select    [:*]
      :from      [[:tap :T]]
      :left-join [[:user :U] [:= :U.id :T.user_id]]
      :where     [:and
                  [:= :U.username user]
                  [:> cursor :T.id]]
      :order-by  [[:created_at :desc]]
      :limit     limit}))))

(comment
  (require '[keg-party.system :as system])
  (def ds (:parts.next.jdbc.core/datasource (system/system)))

  (favorite ds 1 1)
  (read-tap ds 1)
  (unfavorite ds 1 1)
  (read-tap ds 1)

  (create-user! ds {:username "mbastian" :email "markbastian@gmail.com"})
  (create-user! ds {:username "foo" :email "markbastian@gmail.com"})
  (read-users ds)
  (user ds {:id 2})
  (read-user ds 2)
  (read-user ds 0)
  (read-user-by-username ds "bob")
  (read-user-by-email ds "markbastian@gmail.com")
  (sql/get-by-id ds :user 2)
  (create-tap! ds {:user-id 3
                   :tap     '(reduce + (range 10))})
  (read-taps ds)
  (read-tap ds 1)
  (delete-tap! ds 36)
  (count (get-recent-taps ds "mbastian"))
  (map :tap/id (get-recent-taps ds "mbastian"))
  (map :tap/id (get-recent-taps ds "mbastian" 10 16)))
