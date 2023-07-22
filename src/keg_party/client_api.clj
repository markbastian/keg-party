(ns keg-party.client-api
  (:require [clojure.core.async :as async]
            [clojure.tools.logging :as log]
            [ring.adapter.jetty9 :as jetty]))

(defprotocol IClientManager
  (add-client! [this client])
  (remove-client! [this client-id])
  (clients [this])
  (client [this client-id]))

(defn keepalive [client-manager {:keys [client-id]}]
  (async/go-loop []
    (async/<! (async/timeout (* 2 60 1000)))
    (when-let [{:keys [ws]} (client client-manager client-id)]
      (jetty/ping! ws client-id)
      (recur))))

(defrecord AtomicClientManager [state])

(extend-type AtomicClientManager
  IClientManager
  (add-client! [{:keys [state] :as this} {:keys [client-id] :as m}]
    (if-not (client this client-id)
      (do
        (log/debugf "Adding client: %s" client-id)
        (swap! state assoc client-id m)
        (keepalive this m))
      (log/debugf "Client '%s' already exists. Not adding." client-id)))
  (remove-client! [{:keys [state] :as this} client-id]
    (when-some [{:keys [client-id]} (client this client-id)]
      (log/debugf "Removing client: %s" client-id)
      (swap! state dissoc client-id)))
  (clients [{:keys [state]}] @state)
  (client [{:keys [state]} client-id] (@state client-id)))

(defn atomic-client-manager []
  (map->AtomicClientManager {:state (atom {})}))

(defmulti send! (fn [{:keys [transport] :as _user} _message] transport))

(defmethod send! :ws [{:keys [client-id ws]} message]
  (log/infof "Sending to %s via ws." client-id)
  (jetty/send! ws message))

(defn broadcast! [clients client-ids message]
  (doseq [client-id client-ids :let [client (clients client-id)] :when client]
    (send! client message)))
