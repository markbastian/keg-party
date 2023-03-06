(ns keg-party.domain
  (:require [keg-party.htmx-notifications :as htmx-notifications]
            [clojure.set :refer [rename-keys]]
            [honey.sql :as hsql]
            [next.jdbc.sql :as sql]))

(defn create-tap-message! [{:keys [clients _conn]}
                           {:keys [client-id message-id stack message] :as m}]
  {:pre [client-id message-id stack message]}
  (htmx-notifications/broadcast-tapped-data clients nil m))

(defn delete-tap-message! [{:keys [clients _conn]} message-id]
  (htmx-notifications/broadcast-delete-data clients nil message-id))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; Users ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def user-domain->sql-keys {:username  :name})

(def select-user-sql {:select [:*] :from :user})

(def user-sql->domain-keys {:name           :username
                            :user/uuid      :uuid
                            :user/name      :username
                            :user/username  :username})

(defn get-user [this user]
  (let [{:keys [name uuid]} (rename-keys user user-domain->sql-keys)]
    (when-some [where (cond
                        uuid [:= :user.uuid uuid]
                        name [:= :user.name name])]
      (let [sql (assoc select-user-sql
                       :select [[:user.uuid :uuid]
                                [:user.name :username]]
                       :where where)]
        (-> (sql/query this (hsql/format sql))
            first
            (rename-keys user-sql->domain-keys))))))

(defn insert-user! [this {:keys [uuid] :as user}]
  (let [sql-user (cond-> (rename-keys user user-domain->sql-keys)
                   (nil? uuid)
                   (assoc :uuid (random-uuid)))]
    (sql/insert! this :user sql-user)
    (get-user this sql-user)))

(defn update-user! [this user]
  (let [{:keys [uuid name] :as sql-user} (rename-keys user user-domain->sql-keys)]
    (cond
      uuid (sql/update! this :user sql-user ["uuid = ?" uuid])
      name (sql/update! this :user sql-user ["name = ?" name]))
    (get-user this sql-user)))

(defn upsert-user! [this user]
  (let [sql-user (rename-keys user user-domain->sql-keys)]
    (if-some [uuid (:uuid (get-user this sql-user))]
      (update-user! this (assoc sql-user :uuid uuid))
      (insert-user! this sql-user))))

(defn delete-user! [this {:keys [uuid]}]
  (sql/delete! this :user ["uuid = ?" uuid]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; Messages ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def message-sql->domain-keys {:message/uuid    :uuid
                               :message/message :message
                               :message/nanos   :nanos})

(def select-message-sql {:select [:message
                                  :message.uuid
                                  [:user.name :user_name]
                                  :nanos]
                         :join   [:user [:= :message.user_uuid :user.uuid]]
                         :from   :message})

(defn get-message [this {:keys [uuid]}]
  (when uuid
    (let [sql (assoc select-message-sql :where [:= :message.uuid uuid])]
      (-> (sql/query this (hsql/format sql))
          first
          (rename-keys message-sql->domain-keys)))))

(defn get-messages [this & wheres]
  (tap> wheres)
  (let [sql select-message-sql]
    (->> (sql/query this (hsql/format sql))
         (map #(rename-keys % message-sql->domain-keys)))))

(defn insert-message! [this {:keys [uuid nanos] :as message}]
  (let [sql-message (cond-> message
                      (nil? uuid)
                      (assoc :uuid (random-uuid))
                      (nil? nanos)
                      (assoc :nanos (System/nanoTime)))
        {:keys [username user_uuid] :as sql-message} sql-message
        sql-message (cond-> sql-message
                      (and username (nil? user_uuid))
                      (assoc :user_uuid (:uuid (get-user this {:name username}))))]
    (sql/insert! this :message (select-keys sql-message [:uuid :message :user_uuid :nanos]))
    (get-message this sql-message)))

(defn delete-message! [this {:keys [uuid]}]
  (sql/delete! this :messages ["uuid = ?" uuid]))
