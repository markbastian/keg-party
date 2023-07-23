(ns keg-party.web
  (:require [keg-party.commands]
            [keg-party.pages :as pages]
            [clojure.tools.logging :as log]
            [generic.commands :as cmd]
            [generic.utils :as u]
            [generic.web :as gweb]
            [hiccup.core :refer [html]]
            [reitit.ring :as ring]
            [reitit.ring.coercion :as coercion]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.parameters :as parameters]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.middleware.json :refer [wrap-json-response]]
            [ring.util.http-response :refer [not-found ok]]))

(defn landing-page-handler [request]
  (log/info "Returning landing page")
  (ok (pages/landing-page-html request)))

(defn post-message-handler [{:keys [body] :as request}]
  (log/info "Posting message")
  (let [{:keys [message-id] :as m}
        (-> (u/read-json body)
            (select-keys [:client-id :message-id :message])
            (update :message u/base64-decode)
            (assoc :command :tap-message))]
    (cmd/dispatch-command request m)
    (ok message-id)))

(defn show-expand-collapse-handler [{:keys [params]}]
  (let [{:keys [show-collapse target-id]} params]
    (ok (html
         (pages/expand-collapse-block
          (parse-boolean show-collapse)
          target-id)))))

(def routes
  [["/" {:get  landing-page-handler
         :post post-message-handler}]
   ["/collapse" {:post show-expand-collapse-handler}]
   gweb/route
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
