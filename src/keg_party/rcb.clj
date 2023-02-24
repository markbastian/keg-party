(ns keg-party.rcb
  (:require [keg-party.commands :as commands]
            [keg-party.system :as system]
            [parts.datascript.core.core :as ds]
            [parts.state :as ps]))

(def chat-state-key [::system/chat-state ::ds/conn])
(def client-state-key [::system/clients-state ::ps/atom])

(comment
  (let [conn    (get (system/system) chat-state-key)
        clients (get (system/system) client-state-key)]
    (commands/dispatch-command
     {:conn conn :clients clients}
     {:command      :tap-message
      :client-id    "Mark"
      :message "What's going on!!!aaa"})))
