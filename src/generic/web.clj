(ns generic.web
  (:require [clojure.tools.logging :as log]
            [ring.adapter.jetty9 :as jetty]
            [ring.util.http-response :refer [found internal-server-error]]))

(defn ws-upgrade-handler [{:keys [ws-handlers] :as context} upgrade-request]
  (let [{:keys [on-connect on-text on-bytes on-close on-ping on-pong on-error]} ws-handlers
        provided-subprotocols (:websocket-subprotocols upgrade-request)
        provided-extensions   (:websocket-extensions upgrade-request)]
    {:on-connect  (partial on-connect context)
     :on-text     (partial on-text context)
     :on-bytes    (partial on-bytes context)
     :on-close    (partial on-close context)
     :on-ping     (partial on-ping context)
     :on-pong     (partial on-pong context)
     :on-error    (partial on-error context)
     :subprotocol (first provided-subprotocols)
     :extensions  provided-extensions}))

(defn ws-handler
  ([{{:keys [username session-id]} :session :as request}]
   (if (and username session-id)
     (do
       (log/info "Upgrading request to web socket")
       (if (jetty/ws-upgrade-request? request)
         (jetty/ws-upgrade-response (partial ws-upgrade-handler request))
         (internal-server-error "Cannot upgrade request")))
     (do
       (log/info "No session information available")
       (found "/login"))))
  ([request resp _raise]
   (resp (ws-handler request))))

(def route
  ["/ws" {:handler ws-handler}])
