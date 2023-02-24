(ns keg-party.web
  (:require [keg-party.commands :as commands]
            [keg-party.pages :as chat-pages]
            [keg-party.utils :as u]
            [clojure.tools.logging :as log]
            [reitit.ring :as ring]
            [reitit.ring.coercion :as coercion]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.parameters :as parameters]
            [ring.adapter.jetty9 :as jetty]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.middleware.json :refer [wrap-json-response]]
            [ring.util.http-response :refer [internal-server-error not-found ok]]))

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
  ([request]
   (log/info "Upgrading request to web socket")
   (if (jetty/ws-upgrade-request? request)
     (jetty/ws-upgrade-response (partial ws-upgrade-handler request))
     (internal-server-error "Cannot upgrade request")))
  ([request resp _raise]
   (resp (ws-handler request))))

(defn chatroom-page-handler [request]
  (log/info "Returning landing page")
  (ok (chat-pages/landing-page request)))

(defn post-message-handler [{:keys [body] :as request}]
  (log/info "Posting message")
  (let [{:keys [client-id message]} (u/read-json body)
        message (u/base64-decode message)]
    (commands/dispatch-command
     request
     {:command      :tap-message
      :client-id    client-id
      :message message})
    (ok message)))

(def routes
  [["/" {:get  chatroom-page-handler
         :post post-message-handler}]
   ["/ws/:client-id" {:handler    ws-handler
                      :parameters {:path {:client-id string?}}}]
   ["/public/*" (ring/create-file-handler {:root "resources"})]])

(def handler
  (ring/ring-handler
   (ring/router
    routes
    {:data {:middleware [[wrap-defaults
                          (-> site-defaults
                              (update :security dissoc :anti-forgery)
                              (update :security dissoc :content-type-options)
                              (update :responses dissoc :content-types))]
                         ;wrap-params
                         wrap-json-response
                         parameters/parameters-middleware
                         muuntaja/format-request-middleware
                         coercion/coerce-response-middleware
                         coercion/coerce-request-middleware]}})
   ;(ring/create-file-handler {:path "/resources/"})
   (constantly (not-found "Not found"))))
