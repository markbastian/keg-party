(ns keg-party.clients.websocket-client
  (:require [environ.core :refer [env]]
            [generic.utils :as u]
            [hato.websocket :as ws]
            [nano-id.core :refer [nano-id]])
  (:import java.util.Date))

#_:clj-kondo/ignore
(comment
  (let [ws @(ws/websocket "ws://localhost:3333/ws/x"
                          {:on-message (fn [ws msg last?]
                                         (println "Received message:" msg))
                           :on-close   (fn [ws status reason]
                                         (println "WebSocket closed!"))})]
    (ws/send! ws (u/to-json-str
                  {:command    "tap-message"
                   :session-id  (env :user)
                   :message-id (nano-id 10)
                   :message    (str "TIME: " (Date.))}))
    (Thread/sleep 1000)
    (ws/close! ws)))
