(ns keg-party.commands
  (:require [keg-party.domain :as domain]
            [clojure.pprint :as pp]
            [clojure.tools.logging :as log]))

(defmulti dispatch-command (fn [_ctx {:keys [command]}] command))

(defmethod dispatch-command :default [_ {:keys [command] :as cmd}]
  (let [cmdstr (with-out-str (pp/pprint cmd))]
    (log/warnf "Unhandled command: %s\n%s" command cmdstr)))

(defmethod dispatch-command :chat-message
  [context {:keys [username chat-message]}]
  (domain/create-chat-message! (update context :clients deref) username chat-message))
