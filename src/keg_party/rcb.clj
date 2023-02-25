(ns keg-party.rcb
  (:require [keg-party.commands :as commands]
            [keg-party.system :as system]
            [parts.state :as ps]
            [ring.adapter.jetty9 :as jetty]))

(def client-state-key [::system/clients-state ::ps/atom])

(comment
  (let [clients (get (system/system) client-state-key)]
    (commands/dispatch-command
     {:clients clients}
     {:command      :tap-message
      :client-id    "Mark"
      :message "What's going on!!!"}))

  (let [clients (get (system/system) client-state-key)]
    (-> @clients vals first :ws (jetty/ping! (.getBytes "XXX")))))
