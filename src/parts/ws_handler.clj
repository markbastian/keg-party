(ns parts.ws-handler
  (:require
   [clojure.tools.logging :as log]
   [integrant.core :as ig]
   [ring.adapter.jetty9 :as jetty])
  (:import (java.nio ByteBuffer)))

(defn on-bytes [_context _ _ _ _]
  (println "on-bytes unhandled"))

(defn on-ping [_context ws ^ByteBuffer bytebuffer]
  (jetty/send! ws bytebuffer))

(defn on-pong [_context _ws ^ByteBuffer bytebuffer]
  (let [message (loop [res []]
                  (if (.hasRemaining bytebuffer)
                    (recur (conj res (.get bytebuffer)))
                    (String. (byte-array res))))]
    (log/tracef "Received pong: %s" message)))

(defmethod ig/init-key ::ws-handlers [_ config]
  (log/debug "Configuring websocket handlers.")
  (merge
   {:on-bytes #'on-bytes
    :on-ping  #'on-ping
    :on-pong  #'on-pong}
   config))
