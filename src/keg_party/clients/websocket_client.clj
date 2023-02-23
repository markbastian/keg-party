(ns keg-party.clients.websocket-client
  (:require [keg-party.utils :as u]
            [hato.websocket :as ws])
  (:import java.util.Date))

#_:clj-kondo/ignore
(comment
  (let [ws @(ws/websocket "ws://localhost:3000/ws/x"
                          {:on-message (fn [ws msg last?]
                                         (println "Received message:" msg))
                           :on-close   (fn [ws status reason]
                                         (println "WebSocket closed!"))})]
    (ws/send! ws (u/to-json-str
                  {:HEADERS      {:HX-Trigger-Name "chat-message"}
                   :chat-message (str "TIME: " (Date.))}))
    (Thread/sleep 1000)
    (ws/close! ws)))
