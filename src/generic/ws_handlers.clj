(ns generic.ws-handlers
  (:require [generic.client-api :as client-api]
            [generic.commands :as cmd]
            [generic.utils :as u]
            [clojure.tools.logging :as log]))

(defn on-connect [{:keys [path-params client-manager]} ws]
  (let [{:keys [client-id]} path-params]
    (client-api/add-client!
     client-manager
     (client-api/ws-client {:client-id client-id :accept :htmx :ws ws}))))

(defn on-text [{:keys [path-params] :as context} _ws text-message]
  (let [{:keys [client-id]} path-params
        json    (u/read-json text-message)
        command (-> json
                    (update :command keyword)
                    (assoc :client-id client-id))]
    (log/debugf "client-id: %s command: %s" client-id command)
    (cmd/dispatch-command context command)))

(defn on-close [{:keys [path-params client-manager]} _ws _status-code _reason]
  (let [{:keys [client-id]} path-params]
    (log/debugf "on-close triggered for client: %s" client-id)
    (client-api/remove-client! client-manager client-id)))

(defn on-error [{:keys [path-params client-manager]} ws err]
  (let [{:keys [client-id]} path-params]
    (log/debugf "on-error triggered for client: %s" client-id)
    (client-api/remove-client! client-manager client-id)
    (println ws)
    (println err)
    (println "ERROR")))
