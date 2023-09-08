(ns keg-party.web
  (:require
   [keg-party.auth :as auth]
   [keg-party.commands]
   [keg-party.pages :as pages]
   [keg-party.pages.detail :as detail]
   [keg-party.pages.feed :as feed]
   [keg-party.repository :as repository]
   [clojure.edn :as edn]
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
   [ring.util.codec :as codec]
   [ring.util.http-response :refer [forbidden found not-found ok unauthorized]]))

(defn post-message-handler [{{:strs [authorization]} :headers :keys [repo body] :as request}]
  (log/info "Posting message")
  (let [[_ tok] (str/split authorization #" ")
        [username password] (str/split (u/base64-decode tok) #":")]
    (if-some [user (repository/user repo {:username username})]
      (if (auth/check-password (:user/password user) password)
        (let [message (u/base64-decode (slurp body))]
          (cmd/dispatch-command
           request
           {:message  message
            :username username
            :command  :tap-message})
          (ok "ACK"))
        (forbidden "Pound sand"))
      (unauthorized "Pound sand"))))

(defn show-expand-collapse-handler [{:keys [params]}]
  (let [{:keys [show-collapse target-id]} params]
    (ok (html
         (feed/expand-collapse-block
          (parse-boolean show-collapse)
          target-id)))))

(defn basic-auth [{{:strs [authorization]} :headers :keys [repo]}]
  (when (string? authorization)
    (when-some [tok (second (str/split authorization #" "))]
      (let [[username password] (str/split (u/base64-decode tok) #":")]
        (when-some [user (repository/user repo {:username username})]
          (auth/check-password (:user/password user) password))))))

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
                       (ok (pages/clients-page request)))}]
   ["/favorite" {:post   (fn [{{:keys [username message-id]} :params
                               :as                           request}]
                           (cmd/dispatch-command
                            request
                            {:command    :create-favorite-tap
                             :username   username
                             :message-id message-id})
                           ;; Push an optimistic UI update.
                           (ok (html (feed/favorite-tap-block username message-id true))))
                 :delete (fn [{{:keys [username message-id]} :params
                               :as                           request}]
                           (cmd/dispatch-command
                            request
                            {:command    :delete-favorite-tap
                             :username   username
                             :message-id message-id})
                           ;; Push an optimistic UI update.
                           (ok (html (feed/favorite-tap-block username message-id false))))}]
   ["/feed" {:get (fn [request]
                    (ok (pages/feed-page request)))}]
   ["/login" {:get  (fn [request]
                      (ok (pages/login-page request)))
              :post (fn [{{:keys [username password]} :params :keys [repo]}]
                      (if-some [user (repository/user repo {:username username})]
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
                       (ok (pages/signup-page request)))
               :post (fn [{{:keys [username email password]} :params
                           :keys                             [repo]}]
                       (if (repository/user repo {:email email :username username})
                         (found "/login")
                         (try
                           (repository/create-user!
                            repo
                            {:username username
                             :email    email
                             :password (auth/hash-password password)})
                           (found "/login")
                           (catch Exception _
                             (found "/signup")))))}]
   ["/tap/{*path}" {:get  (fn [{{:keys [path]} :path-params
                                :keys          [repo] :as request}]
                            (let [[tap-id & r] (->> (str/split path #"/")
                                                    (map (comp edn/read-string codec/url-decode)))
                                  drill-path (vec r)]
                              (if-some [tap (repository/tap repo {:id tap-id})]
                                (let [tap-data (edn/read-string (:tap/tap tap))]
                                  (ok
                                   (pages/tap-detail-page
                                    request
                                    (assoc tap :tap/data tap-data)
                                    drill-path)))
                                (not-found "Pound Sand"))))
                    :post (fn [{{:keys [path]} :path-params
                                :keys          [form-params repo]}]
                            (let [[tap-id & r] (->> (str/split path #"/")
                                                    (map (comp edn/read-string codec/url-decode)))]
                              (if-some [tap (repository/tap repo {:id tap-id})]
                                (let [drill-path (vec r)
                                      tap-data (edn/read-string (:tap/tap tap))
                                      selected (set (mapv edn/read-string (keys form-params)))]
                                  (ok (html
                                       (detail/detail-code-block
                                        (assoc tap :tap/data tap-data)
                                        drill-path
                                        selected))))
                                (not-found "Pound Sand"))))}]
   ["/tap_page" {:get (fn [{{:keys [username]}     :session
                            {:keys [limit cursor]} :params
                            :keys                  [repo] :as request}]
                        (ok
                         (html
                          (let [recent-taps (repository/get-recent-taps repo username limit cursor)
                                tap-count   (dec (count recent-taps))]
                            (map-indexed
                             (fn [idx {:tap/keys [tap id]}]
                               (feed/code-block
                                request username id tap (= idx tap-count)))
                             recent-taps)))))}]
   gweb/route
   ["/public/*" (ring/create-resource-handler {:root "resources"})]])

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
