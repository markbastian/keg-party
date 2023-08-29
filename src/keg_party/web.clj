(ns keg-party.web
  (:require
   [keg-party.auth :as auth]
   [keg-party.commands]
   [keg-party.migrations :as migrations]
   [keg-party.pages :as pages]
   [clojure.string :as str]
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

(defn post-message-handler [{{:strs [authorization]} :headers :keys [ds body] :as request}]
  (log/info "Posting message")
  (let [[_ tok] (str/split authorization #" ")
        [username password] (str/split (u/base64-decode tok) #":")]
    (if-some [user (migrations/user ds {:username username})]
      (if (auth/check-password (:user/password user) password)
        (do
          (cmd/dispatch-command
           request
           {:message  (u/base64-decode (slurp body))
            :username username
            :command  :tap-message})
          (ok "ACK"))
        (forbidden "Pound sand"))
      (unauthorized "Pound sand"))))

(defn show-expand-collapse-handler [{:keys [params]}]
  (let [{:keys [show-collapse target-id]} params]
    (ok (html
         (pages/expand-collapse-block
          (parse-boolean show-collapse)
          target-id)))))

(defn basic-auth [{{:strs [authorization]} :headers :keys [ds]}]
  (let [[_ tok] (str/split authorization #" ")
        [username password] (str/split (u/base64-decode tok) #":")]
    (when-some [user (migrations/user ds {:username username})]
      (auth/check-password (:user/password user) password))))

(defn wrap-auth [handler whitelist]
  (fn [{{:keys [username]} :session
        :keys              [uri] :as request}]
    (if (or (whitelist uri)
            username
            (basic-auth request))
      (handler request)
      (found "/login"))))

(def routes
  [["/" {:get  (fn [_request]
                 (found "/feed"))
         :post post-message-handler}]
   ["/collapse" {:post show-expand-collapse-handler}]
   ["/clients" {:get (fn [request]
                       (ok (pages/wrap-as-page
                            (pages/clients-page request))))}]
   ["/favorite" {:post   (fn [{{:keys [username message-id]} :params}]
                           (ok (html (pages/favorite-tap-block username message-id true))))
                 :delete (fn [{{:keys [username message-id]} :params}]
                           (ok (html (pages/favorite-tap-block username message-id false))))}]
   ["/feed" {:get (fn [request]
                    (ok (pages/wrap-as-page
                         (pages/feed-page request))))}]
   ["/login" {:get  (fn [request]
                      (ok (pages/wrap-as-page
                           (pages/login-page request))))
              :post (fn [{{:keys [username password]} :params :keys [ds]}]
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
                           :keys                             [ds]}]
                       (if (migrations/user ds {:email email :username username})
                         (found "/login")
                         (try
                           (migrations/create-user!
                            ds
                            {:username username
                             :email    email
                             :password (auth/hash-password password)})
                           (found "/login")
                           (catch Exception _
                             (found "/signup")))))}]
   ["/tap_page" {:get (fn [{{:keys [username]}     :session
                            {:keys [limit cursor]} :params
                            :keys                  [ds]}]
                        (ok
                         (html
                          (let [recent-taps (migrations/get-recent-taps ds username limit cursor)
                                tap-count   (dec (count recent-taps))]
                            (map-indexed
                             (fn [idx {:tap/keys [tap id]}]
                               (pages/code-block username id tap (= idx tap-count)))
                             recent-taps)))))}]
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
                              (assoc-in [:session :cookie-name] "keg-party-sessions")
                                ;; Set the session to last for a year. It will end when browser is closed.
                              (assoc-in [:session :cookie-attrs] {:max-age (* 60 24 365)}))]
                         wrap-json-response
                         parameters/parameters-middleware
                         muuntaja/format-request-middleware
                         coercion/coerce-response-middleware
                         coercion/coerce-request-middleware
                         [wrap-auth #{"/signup" "/login" "/logout"}]]}})
    ;(ring/create-file-handler {:path "/resources/"})
   (constantly (not-found "Not found"))))
