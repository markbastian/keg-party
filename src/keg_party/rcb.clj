(ns keg-party.rcb
  (:require [keg-party.commands :as commands]
            [keg-party.domain :as domain]
            [keg-party.system :as system]
            [parts.next.jdbc.core :as parts.jdbc]
            [parts.state :as ps]
            [ring.adapter.jetty9 :as jetty]))

(def client-state-key [::system/clients-state ::ps/atom])
(def jdbc-key ::parts.jdbc/datasource)

(comment
  (let [clients (get (system/system) client-state-key)]
    (commands/dispatch-command
     {:clients clients}
     {:command   :tap-message
      :client-id "Mark"
      :message   "What's going on!!!"}))

  (let [clients (get (system/system) client-state-key)]
    (-> @clients vals first :ws (jetty/ping! (.getBytes "XXX"))))

  (let [conn (get (system/system) jdbc-key)]
    (domain/insert-user! conn {:username "Mark"}))

  (let [conn (get (system/system) jdbc-key)]
    (domain/get-user conn {:username "Mark"}))

  (let [conn (get (system/system) jdbc-key)
        {:keys [name] :as _user} (domain/get-user conn {:username "Mark"})]
    (domain/insert-message! conn {:message  "efafsfasfaesf"
                                  :username name}))

  (let [conn (get (system/system) jdbc-key)
        {:keys [uuid] :as _user} (domain/get-user conn {:username "Mark"})]
    (domain/insert-message! conn {:message   "efafsfasfaesf"
                                  :user_uuid uuid}))

  (let [conn (get (system/system) jdbc-key)
        {:keys [uuid] :as _user} (domain/get-user conn {:username "Mark"})]
    (domain/get-messages conn :where [:= :user_uuid uuid])))
