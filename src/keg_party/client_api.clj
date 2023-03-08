(ns keg-party.client-api
  (:require [clojure.core.async :as async]
            [clojure.tools.logging :as log]
            [ring.adapter.jetty9 :as jetty]))

(defn keepalive [state {:keys [client-id]}]
  (async/go-loop []
    (async/<! (async/timeout (* 2 60 1000)))
    (when-let [{:keys [ws]} (@state client-id)]
      (jetty/ping! ws client-id)
      (recur))))

(defn add-client! [state {:keys [client-id] :as m}]
  (if-not (@state client-id)
    (do
      (log/debugf "Adding client: %s" client-id)
      (swap! state assoc client-id m)
      (keepalive state m))
    (log/debugf "Client '%s' already exists. Not adding." client-id)))

(defn remove-client! [state client-id]
  (when-some [{:keys [client-id]} (@state client-id)]
    (log/debugf "Removing client: %s" client-id)
    (swap! state dissoc client-id)))

(defmulti send! (fn [{:keys [transport] :as _user} _message] transport))

(defmethod send! :ws [{:keys [client-id ws]} message]
  (log/infof "Sending to %s via ws." client-id)
  (jetty/send! ws message))

(defn broadcast! [clients client-ids message]
  (doseq [client-id client-ids :let [client (clients client-id)] :when client]
    (send! client message)))
