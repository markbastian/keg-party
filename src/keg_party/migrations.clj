(ns keg-party.migrations
  (:require
   [honey.sql :as hsql]))

(def create-channel-table-ddl
  (hsql/format
   {:create-table [:channel :if-not-exists]
    :with-columns
    [[:id :integer :primary-key]
     [:name :varchar [:not nil] :unique]
     [:created_at :timestamp [:not nil]
      [:default :current_timestamp]]]}
   {:pretty true :quoted true}))

(def create-user-table-ddl
  (hsql/format
   {:create-table [:user :if-not-exists]
    :with-columns
    [[:id :integer :primary-key]
     [:username :text [:not nil] :unique]
     [:email :text [:not nil] :unique]
     [:channel_id :int [:not nil]]
     [:password :text [:not nil]]
     [:created_at :timestamp [:not nil]
      [:default :current_timestamp]]
     [[:foreign-key :channel_id] [:references :channel :id]]]}
   {:pretty true
     ;:dialect :ansi
    }))

(def create-tap-table-ddl
  (hsql/format
   {:create-table [:tap :if-not-exists]
    :with-columns
    [[:id :integer :primary-key]
     [:user_id :int [:not nil]]
     [:channel_id :int [:not nil]]
     [:created_at :timestamp [:not nil] [:default :current_timestamp]]
     [:tap :text [:not nil]]
     [[:foreign-key :user_id] [:references :user :id]]
     [[:foreign-key :channel_id] [:references :channel :id]]]}
   #_{:pretty true :dialect :ansi}))

(def create-favorite-table-ddl
  (hsql/format
   {:create-table [:favorite :if-not-exists]
    :with-columns
    [[:user_id :int [:not nil]]
     [:tap_id :int [:not nil]]
     [[:foreign-key :user_id] [:references :user :id]]
     [[:foreign-key :tap_id] [:references :tap :id]]
     [[:primary-key :user_id :tap_id]]]}
   #_{:pretty true :dialect :ansi}))

(def migrations
  [create-channel-table-ddl
   create-user-table-ddl
   create-tap-table-ddl
   create-favorite-table-ddl])
