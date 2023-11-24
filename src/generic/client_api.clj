(ns generic.client-api
  (:require
   [clojure.core.async :as async]
   [clojure.tools.logging :as log]
   [ring.adapter.jetty9 :as jetty]))

(defprotocol IClientManager
  (add-client! [this client])
  (remove-client! [this session-id])
  (clients [this] [this username])
  (client [this session-id]))

(defrecord AtomicClientManager [state])

(extend-type AtomicClientManager
  IClientManager
  (add-client! [{:keys [state] :as this} {:keys [session-id] :as m}]
    (if-not (client this session-id)
      (do
        (log/debugf "Adding client: %s" session-id)
        (swap! state assoc session-id m))
      (log/debugf "Client '%s' already exists. Not adding." session-id)))
  (remove-client! [{:keys [state] :as this} session-id]
    (if-some [{:keys [session-id keepalive]} (client this session-id)]
      (do
        (when keepalive
          (keepalive))
        (log/debugf "Removing client: %s" session-id)
        (swap! state dissoc session-id))
      (log/debugf "No client to remove for id: %s" session-id)))
  (clients
    ([{:keys [state]}] (vals @state))
    ([{:keys [state]} username]
     (for [client (vals @state)
           :when (= username (:username client))]
       client)))
  (client [{:keys [state]} session-id] (@state session-id)))

(defn atomic-client-manager []
  (map->AtomicClientManager {:state (atom {})}))

(defprotocol IClient
  (send! [this message]))

(defrecord WebSocketClient [ws username session-id])

(defn keepalive [{:keys [ws session-id]}]
  {:pre [ws session-id]}
  (let [alive? (atom true)]
    (async/go-loop []
      ;; Send a ping every 5 minutes
      ;; (async/<! (async/timeout 1000))
      (async/<! (async/timeout (* 5 60 1000)))
      (when @alive?
        (try
          (log/infof "Sending ping to client %s" session-id)
          (jetty/ping! ws session-id)
          (catch Exception _
            (swap! alive? (constantly false))
            (log/infof "Stopping ping for client %s" session-id)))
        (recur)))
    (fn []
      (log/infof "Stopping keepalive for client %s" session-id)
      (reset! alive? false))))

(extend-type WebSocketClient
  IClient
  (send! [{:keys [session-id ws]} message]
    (log/infof "Sending %s to %s via ws." message session-id)
    (try
      (jetty/send! ws message)
      (catch Exception _
        (log/warnf
         "Failed to send message:\n%s" message)))))

(defn ws-client [{:keys [ws username session-id] :as client-map}]
  {:pre [ws username session-id]}
  (keepalive client-map)
  (map->WebSocketClient
   (assoc client-map
          :keepalive (keepalive client-map))))

(defn broadcast! [clients message]
  (doseq [client clients]
    (send! client message)))
