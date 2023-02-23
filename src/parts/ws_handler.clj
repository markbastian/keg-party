(ns parts.ws-handler
  (:require
   [clojure.tools.logging :as log]
   [integrant.core :as ig]
   [ring.adapter.jetty9 :as jetty]))

(defn on-bytes [_context _ _ _ _]
  (println "on-bytes unhandled"))

(defn on-ping [_context ws payload] (println "PING")
  (jetty/send! ws payload))

(defn on-pong [_context _ _] (println "PONG"))

(defmethod ig/init-key ::ws-handlers [_ config]
  (log/debug "Configuring websocket handlers.")
  (merge
   {:on-bytes #'on-bytes
    :on-ping  #'on-ping
    :on-pong  #'on-pong}
   config))
