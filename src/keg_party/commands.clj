(ns keg-party.commands
  (:require [keg-party.events :as events]
            [clojure.pprint :as pp]
            [clojure.tools.logging :as log]))

(defmulti dispatch-command (fn [_ctx {:keys [command]}] command))

(defmethod dispatch-command :default [_ {:keys [command] :as cmd}]
  (let [cmdstr (with-out-str (pp/pprint cmd))]
    (log/warnf "Unhandled command: %s\n%s" command cmdstr)))

(defmethod dispatch-command :tap-message
  [context {:keys [command client-id message-id message] :as m}]
  {:pre [client-id message-id message]}
  (log/infof "Dispatching command: %s" command)
  (events/create-tap-message! (update context :clients deref) m))

(defmethod dispatch-command :delete-message
  [context {:keys [command message-id]}]
  (log/infof "Dispatching command: %s" command)
  (events/delete-tap-message! (update context :clients deref) message-id))
