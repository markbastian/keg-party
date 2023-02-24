(ns keg-party.ws-handlers
  (:require [keg-party.client-api :as client-api]
            [keg-party.commands :as commands]
            [keg-party.utils :as u]
            [clojure.tools.logging :as log]))

(defn on-connect [{:keys [path-params clients]} ws]
  (let [{:keys [client-id]} path-params]
    (client-api/add-client! clients {:client-id client-id
                                     :transport :ws
                                     :ws        ws})))

(defn on-text [{:keys [path-params] :as context} _ws text-message]
  (let [{:keys [client-id]} path-params
        {:keys [HEADERS] :as json} (u/read-json text-message)
        command (-> json
                    (assoc
                     :client-id client-id
                     :command (some-> HEADERS :HX-Trigger-Name keyword))
                    (dissoc :HEADERS))]
    (log/debugf "client-id: %s command: %s" client-id command)
    (commands/dispatch-command context command)))

(defn on-close [{:keys [path-params clients]} _ws _status-code _reason]
  (let [{:keys [client-id]} path-params]
    (log/debugf "on-close triggered for client: %s" client-id)
    (client-api/remove-client! clients client-id)))

(defn on-error [{:keys [path-params clients]} ws err]
  (let [{:keys [client-id]} path-params]
    (log/debugf "on-error triggered for client: %s" client-id)
    (client-api/remove-client! clients client-id)
    (println ws)
    (println err)
    (println "ERROR")))
