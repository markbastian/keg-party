(ns keg-party.web
  (:require
   [keg-party.auth :as auth]
   [keg-party.commands]
   [keg-party.migrations :as migrations]
   [keg-party.pages :as pages]
   [clojure.pprint :as pp]
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
   [ring.middleware.session.cookie :refer [cookie-store]]
   [ring.util.http-response :refer [forbidden found not-found ok unauthorized]]))

(defn landing-page-handler [request]
  (log/info "Returning landing page")
  (ok (pages/landing-page-html request)))

(defn post-message-handler [{:keys [body] :as request}]
  (log/info "Posting message")
  (cmd/dispatch-command
   request
   (-> (u/read-json body)
       (select-keys [:username :message])
       (update :message u/base64-decode)
       (assoc :command :tap-message)
       (doto pp/pprint)))
  (ok "ACK"))

(defn show-expand-collapse-handler [{:keys [params]}]
  (let [{:keys [show-collapse target-id]} params]
    (ok (html
         (pages/expand-collapse-block
          (parse-boolean show-collapse)
          target-id)))))

(def routes
  [["/" {:get  (fn [{:keys [session]}]
                 (if (:username session)
                   (found "/feed")
                   (found "/login")))
         :post post-message-handler}]
   ["/collapse" {:post show-expand-collapse-handler}]
   ["/clients" {:get (fn [{:keys [session] :as request}]
                       (println "XXXXXXXXXXXX")
                       (pp/pprint session)
                       (println "XXXXXXXXXXXX")
                       (if (:username session)
                         (ok (pages/wrap-as-page
                              (pages/clients-page request)))
                         (found "/login")))}]
   ["/feed" {:get (fn [{:keys [session] :as request}]
                    (if (:username session)
                      (ok (pages/wrap-as-page
                           (pages/feed-page request)))
                      (found "/login")))}]
   ["/login" {:get  (fn [request]
                      (ok (pages/wrap-as-page
                           (pages/login-page request))))
              :post (fn [{{:keys [username password]} :params :keys [ds session]}]
                      (pp/pprint [:login session])
                      (if-some [user (migrations/user ds {:username username})]
                        (if (auth/check-password (:user/password user) password)
                          (-> (found "/feed")
                              (update :session assoc
                                      :username username
                                      :session-id (random-uuid)))
                          (forbidden "Pound sand"))
                        (unauthorized "Pound sand")))}]
   ["/logout" {:get (fn [_request]
                      (-> (found "/login")
                          (dissoc :session)))}]
   ["/signup" {:get  (fn [request]
                       (ok (pages/wrap-as-page
                            (pages/signup-page request))))
               :post (fn [{{:keys [username email password]} :params
                           :keys                             [ds session]}]
                       (if (migrations/user ds {:email email :username username})
                         (found "/login")
                         (try
                           (migrations/create-user!
                            ds
                            {:username username
                             :email    email
                             :password (auth/hash-password password)})
                           (-> (found "/feed")
                               (assoc :session (assoc session :username username)))
                           (catch Exception _
                             (found "/signup")))))}]
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
                              (update :responses dissoc :content-types)
                              (assoc-in [:session :store] (cookie-store))
                              (assoc-in [:session :cookie-name] "keg-party-sessions"))]
                         wrap-json-response
                         parameters/parameters-middleware
                         muuntaja/format-request-middleware
                         coercion/coerce-response-middleware
                         coercion/coerce-request-middleware]}})
    ;(ring/create-file-handler {:path "/resources/"})
   (constantly (not-found "Not found"))))
