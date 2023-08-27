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
     [:favorite :bool [:not nil] [:default false]]
     [[:foreign-key :user_id] [:references :user :id]]]}))

(def migrations
  [create-user-table-ddl
   create-tap-table-ddl])

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
      :limit     limit}))))

(comment
  (require '[keg-party.system :as system])
  (let [ds (:parts.next.jdbc.core/datasource (system/system))]
    (create-user! ds {:username "mbastian" :email "markbastian@gmail.com"}))

  (let [ds (:parts.next.jdbc.core/datasource (system/system))]
    (create-user! ds {:username "foo" :email "markbastian@gmail.com"}))

  (let [ds (:parts.next.jdbc.core/datasource (system/system))]
    (read-users ds))

  (let [ds (:parts.next.jdbc.core/datasource (system/system))]
    (user ds {:id 2}))

  (let [ds (:parts.next.jdbc.core/datasource (system/system))]
    (read-user ds 2))

  (let [ds (:parts.next.jdbc.core/datasource (system/system))]
    (read-user ds 0))

  (let [ds (:parts.next.jdbc.core/datasource (system/system))]
    (read-user-by-username ds "bob"))

  (let [ds (:parts.next.jdbc.core/datasource (system/system))]
    (read-user-by-email ds "markbastian@gmail.com"))

  (let [ds (:parts.next.jdbc.core/datasource (system/system))]
    (sql/get-by-id ds :user 2))

  (let [ds (:parts.next.jdbc.core/datasource (system/system))]
    (create-tap! ds {:user-id 3
                     :tap     '(reduce + (range 10))}))

  (let [ds (:parts.next.jdbc.core/datasource (system/system))]
    (read-taps ds))

  (let [ds (:parts.next.jdbc.core/datasource (system/system))]
    (read-tap ds 1))

  (let [ds (:parts.next.jdbc.core/datasource (system/system))]
    (delete-tap! ds 36)))
