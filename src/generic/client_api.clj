(ns generic.client-api
  (:require [clojure.core.async :as async]
            [clojure.tools.logging :as log]
            [ring.adapter.jetty9 :as jetty]))

(defprotocol IClientManager
  (add-client! [this client])
  (remove-client! [this client-id])
  (clients [this] [this username])
  (client [this client-id]))

(defrecord AtomicClientManager [state])

(extend-type AtomicClientManager
  IClientManager
  (add-client! [{:keys [state] :as this} {:keys [client-id] :as m}]
    (if-not (client this client-id)
      (do
        (log/debugf "Adding client: %s" client-id)
        (swap! state assoc client-id m))
      (log/debugf "Client '%s' already exists. Not adding." client-id)))
  (remove-client! [{:keys [state] :as this} client-id]
    (when-some [{:keys [client-id]} (client this client-id)]
      (log/debugf "Removing client: %s" client-id)
      (swap! state dissoc client-id)))
  (clients
    ([{:keys [state]}] (vals @state))
    ([{:keys [state]} username]
     (for [client (vals @state)
           :when (= username (:username client))]
       client)))
  (client [{:keys [state]} client-id] (@state client-id)))

(defn atomic-client-manager []
  (map->AtomicClientManager {:state (atom {})}))

(defprotocol IClient
  (send! [this message]))

(defrecord WebSocketClient [ws username client-id])

(defn keepalive [{:keys [ws client-id]}]
  {:pre [ws client-id]}
  (let [alive? (atom true)]
    (async/go-loop []
      ;; Send a ping every 5 minutes
      (async/<! (async/timeout (* 5 60 1000)))
      (when @alive?
        (try
          (log/infof "Sending ping to client %s" client-id)
          (jetty/ping! ws client-id)
          (catch Exception _
            (swap! alive? (constantly false))
            (log/infof "Stopping ping for client %s" client-id)))
        (recur)))))

(extend-type WebSocketClient
  IClient
  (send! [{:keys [client-id ws]} message]
    (log/infof "Sending to %s via ws." client-id)
    (jetty/send! ws message)))

(defn ws-client [{:keys [ws username client-id] :as client-map}]
  {:pre [ws username client-id]}
  (keepalive client-map)
  (map->WebSocketClient client-map))

(defn broadcast! [clients message]
  (doseq [client clients]
    (send! client message)))
