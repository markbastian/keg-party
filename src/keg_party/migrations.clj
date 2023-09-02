(ns keg-party.migrations
  (:require
   [honey.sql :as hsql]))

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
