(ns keg-party.clients.websocket-client
  (:require [keg-party.utils :as u]
            [environ.core :refer [env]]
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
                  {:HEADERS      {:HX-Trigger-Name "tap-message"}
                   :client-id (env :user)
                   :message-id (nano-id 10)
                   :message (str "TIME: " (Date.))}))
    (Thread/sleep 1000)
    (ws/close! ws)))
