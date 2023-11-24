(ns generic.ws-handlers
  (:require
   [generic.client-api :as client-api]
   [generic.commands :as cmd]
   [generic.utils :as u]
   [clojure.tools.logging :as log]))

(defn on-connect [{{:keys [username session-id]} :session
                   :keys                         [client-manager]
                   :as                           context} ws]
  (let [client (client-api/ws-client {:username   username
                                      :session-id session-id
                                      :accept     :htmx
                                      :ws         ws})
        cmd    {:command    :user-joined
                :username   username
                :session-id session-id
                :client     client}]
    (client-api/add-client! client-manager client)
    (cmd/dispatch-command context cmd)))

(defn on-text [{{:keys [username session-id]} :session
                :as                           context}
               _ws
               text-message]
  (let [json    (u/read-json text-message)
        command (-> json
                    (update :command keyword)
                    (assoc
                     :username username
                     :session-id session-id))]
    (log/debugf "session-id: %s command: %s" session-id command)
    (cmd/dispatch-command context command)))

(defn on-close [{{:keys [username session-id]} :session
                 :keys                         [client-manager]
                 :as                           context}
                _ws
                _status-code
                _reason]
  (log/debugf "on-close triggered for client: %s" session-id)
  (let [cmd {:command    :user-left
             :username   username
             :session-id session-id}]
    (client-api/remove-client! client-manager session-id)
    (cmd/dispatch-command context cmd)))

(defn on-error [{{:keys [username session-id]} :session
                 :keys                         [client-manager]
                 :as                           context}
                ws
                err]
  (log/debugf "on-error triggered for client: %s" session-id)
  (let [cmd {:command    :user-left
             :username   username
             :session-id session-id}]
    (client-api/remove-client! client-manager session-id)
    (cmd/dispatch-command context cmd))
  (println ws)
  (println err)
  (println "ERROR"))
