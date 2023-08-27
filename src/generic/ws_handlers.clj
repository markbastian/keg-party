(ns generic.ws-handlers
  (:require [generic.client-api :as client-api]
            [generic.commands :as cmd]
            [generic.utils :as u]
            [clojure.tools.logging :as log]))

(defn on-connect [{{:keys [username session-id]} :session :keys [client-manager]} ws]
  (client-api/add-client!
   client-manager
   (client-api/ws-client {:client-id session-id
                          :username  username
                          :accept    :htmx
                          :ws        ws})))

(defn on-text [{{:keys [_username session-id]} :session :as context} _ws text-message]
  (let [json    (u/read-json text-message)
        command (-> json
                    (update :command keyword)
                    (assoc :client-id session-id))]
    (log/debugf "client-id: %s command: %s" session-id command)
    (cmd/dispatch-command context command)))

(defn on-close [{{:keys [_username session-id]} :session
                 :keys                          [client-manager]} _ws _status-code _reason]
  (log/debugf "on-close triggered for client: %s" session-id)
  (client-api/remove-client! client-manager session-id))

(defn on-error [{{:keys [_username session-id]} :session
                 :keys                          [client-manager]} ws err]
  (log/debugf "on-error triggered for client: %s" session-id)
  (client-api/remove-client! client-manager session-id)
  (println ws)
  (println err)
  (println "ERROR"))
