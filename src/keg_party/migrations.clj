(ns keg-party.migrations
  (:require [honey.sql :as hsql]))

(def create-user-table-sql
  (hsql/format
   {:create-table [:user :if-not-exists]
    :with-columns
    [[:uuid :uuid :primary-key [:not nil]]
     [:name :varchar :unique [:not nil]]]}))

(def create-message-table-sql
  (hsql/format
   {:create-table [:message :if-not-exists]
    :with-columns
    [[:uuid :uuid :primary-key [:not nil]]
     [:user-uuid :uuid [:not nil]]
     [:message :varchar [:not nil]]
     [:nanos :long [:not nil]]
     [[:foreign-key :user-uuid] [:references :user :uuid]]]}))
