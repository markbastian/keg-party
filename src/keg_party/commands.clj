(ns keg-party.commands
  (:require [keg-party.domain :as domain]
            [clojure.pprint :as pp]
            [clojure.tools.logging :as log]))

(defmulti dispatch-command (fn [_ctx {:keys [command]}] command))

(defmethod dispatch-command :default [_ {:keys [command] :as cmd}]
  (let [cmdstr (with-out-str (pp/pprint cmd))]
    (log/warnf "Unhandled command: %s\n%s" command cmdstr)))

(defmethod dispatch-command :tap-message
  [context {:keys [command client-id message]}]
  (log/infof "Dispatching command: %s" command)
  (domain/create-tap-message! (update context :clients deref) client-id message))
