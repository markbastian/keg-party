(ns keg-party.pages
  "Functions to creat server rendered pages."
  (:require
   [keg-party.migrations :as migrations]
   [generic.client-api :as client-api]
   [generic.utils :as u]
   [hiccup.page :refer [html5 include-css include-js]]))

(defn expand-collapse-block [show-collapse target-id]
  [(if show-collapse
     :i.fa-regular.fa-square-caret-up
     :i.fa-regular.fa-square-caret-down)
   {:type           "button"
    :data-bs-toggle "collapse"
    :data-bs-target target-id
    :hx-post        "/collapse"
    :hx-vals        (u/to-json-str {:show-collapse (not show-collapse)
                                    :target-id     target-id})
    :hx-swap        "outerHTML"}])

(defn favorite-star [username message-id & attributes]
  [:i.fa-solid.fa-star
   (into
    {:id (format "tap-favorite-%s-%s" username message-id)}
    attributes)])

(defn favorite-tap-block [username message-id favorite?]
  (let [action (if favorite? :hx-delete :hx-post)]
    [:button.btn.btn-dark.btn-sm
     {:hx-vals (u/to-json-str {:username   username
                               :message-id message-id})
      action   "/favorite"
      :hx-swap "outerHTML"}
     (if favorite?
       (favorite-star username message-id {:style "color:#FFD700;"})
       (favorite-star username message-id))]))

(defn code-block
  ([context username message-id message]
   (code-block context username message-id message false))
  ([{:keys [ds]} username message-id message last?]
   (let [id (format "code-block-%s" message-id)]
     [:div
      (cond->
       {:id id}
        last?
        (merge {:hx-get     (format "/tap_page?limit=3&cursor=%s" message-id)
                :hx-trigger "revealed"
                :hx-swap    "afterend"}))
      [:div.collapse.show
       {:id (format "%s-collapse" id)}
       [:p username]
       [:div.d-flex.justify-content-between.align-items-top
        [:div.overflow-auto
         [:pre
          [:code.language-clojure message]]
         [:script "hljs.highlightAll();"]]
        [:div.d-flex.flex-column.gap-1
         [:button.btn.btn-dark.btn-sm
          {:onclick (format
                     "navigator.clipboard.writeText(atob('%s'))"
                     (u/base64-encode message))}
          [:i.fa-solid.fa-copy]]
         [:button.btn.btn-dark.btn-sm
          {:ws-send "true"
           :hx-vals (u/to-json-str {:command    :delete-message
                                    :message-id message-id})}
          [:i.fa-solid.fa-trash]]
         (let [favorite? (migrations/get-favorite ds {:username username
                                                      :tap-id   message-id})]
           (println favorite?)
           (favorite-tap-block
            username
            message-id
            favorite?))]]]
      [:div.row.align-items-center
       [:div.col [:hr]]
       [:div.col-auto
        (expand-collapse-block true (format "#%s-collapse" id))]
       [:div.col [:hr]]]])))

(defn notifications-pane [& r]
  (into [:div#tap-log.p-2] r))

(defn navbar [{:keys [session]}]
  [:nav.navbar.navbar-expand-lg.navbar-dark.bg-dark.sticky-top
   [:a.navbar-brand {:href "#"}
    [:img {:src   "public/keg_party/rootbeer-sm.png"
           :width "30" :height "30" :alt ""}]]
   [:button.navbar-toggler {:type           "button"
                            :data-bs-toggle "collapse"
                            :data-bs-target "#navbarText"}
    [:span.navbar-toggler-icon]]
   [:div#navbarText.collapse.navbar-collapse
    [:ul.navbar-nav.me-auto.mb-2.mb-lg-0
     [:li.nav-item
      [:a.nav-link.active {:href "/clients"} "Clients"]]
     [:li.nav-item
      [:a.nav-link {:href "/logout"}
       (format "Logout %s" (:username session))]]]]])

(defn feed-page [{{:keys [username]} :session
                  :keys              [ds] :as request}]
  [:div
   {:hx-ext     "ws"
    :ws-connect (format "/ws")}
   (navbar request)
   (notifications-pane
    (let [recent-taps (migrations/get-recent-taps ds username 3)
          tap-count   (dec (count recent-taps))]
      (map-indexed
       (fn [idx {:tap/keys [tap id]}]
         (code-block request username id tap (= idx tap-count)))
       recent-taps)))])

(defn login-page [& attributes]
  [:div (into
         {:id    "app"
          :style "position:absolute; top:20%; right:0; left:0;"}
         attributes)
   [:form.container.border.rounded
    {:action "/login" :method :post}
    [:div.form-group.mb-2
     [:h4.text-center "Welcome to the party!"]
     [:label "Username"]
     [:input.form-control
      {:name         "username"
       :placeholder  "Enter username"
       :autocomplete "off"}]
     [:label "Password"]
     [:input.form-control
      {:name         "password"
       :type         "password"
       :placeholder  "Enter your password"
       :autocomplete "off"}]]
    [:div.d-grid.gap-2
     [:button.btn.btn-primary.btn-dark
      {:type "submit"}
      "Sign in"]]
    [:p "Don't have an account?" [:a {:href "/signup"} "Sign up"]]]])

(defn signup-page [& attributes]
  [:div (into
         {:id    "app"
          :style "position:absolute; top:20%; right:0; left:0;"}
         attributes)
   [:form.container.border.rounded
    {:action "/signup" :method :post}
    [:div.form-group.mb-2
     [:h4.text-center "Join the party!"]
     [:label "Username"]
     [:input.form-control
      {:name         "username"
       :placeholder  "Enter username"
       :autocomplete "off"}]
     [:label "Email address"]
     [:input.form-control
      {:name         "email"
       :type         "email"
       :placeholder  "Enter email"
       :autocomplete "off"}]
     [:label "Password"]
     [:input.form-control
      {:name         "password"
       :type         "password"
       :placeholder  "Enter a really great password"
       :autocomplete "off"}]]
    [:div.d-grid.gap-2
     [:button.btn.btn-primary.btn-dark
      {:type "submit"}
      "Sign up"]]]])

(defn clients-page [{{:keys [session-id] :as session} :session :keys [client-manager]}]
  [:div
   (into
    [:table.table.table-striped.table-dark.table-bordered.table-sm
     [:tr
      [:th "Username"]
      [:th "Session ID"]
      [:th "Protocol"]]]
    (for [{:keys [client-id username ws]} (client-api/clients client-manager)]
      [:tr
       [:td (cond-> username (= (:username session) username) (str " *"))]
       [:td (cond-> client-id (= session-id client-id) (str " *"))]
       [:td (if (some? ws) "ws" "?")]]))
   [:p [:a {:href "/feed"} "Feed"]]])

(defn wrap-as-page [content]
  (html5
   (include-css
    "https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css"
    "https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.2.1/css/all.min.css"
    "https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.7.0/styles/intellij-light.min.css"
      ;"//cdnjs.cloudflare.com/ajax/libs/highlight.js/11.7.0/styles/default.min.css"
    )
   (include-js
    "https://unpkg.com/htmx.org@1.8.4"
    "https://unpkg.com/htmx.org/dist/ext/ws.js"
    "https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/js/bootstrap.bundle.min.js"
    "public/keg_party/highlight.min.js")
    ;[:script (slurp (io/resource "keg_party/highlight.min.js"))]
   [:script "hljs.highlightAll();"]
   [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
   content))

(defn landing-page-html [_request]
  (wrap-as-page
   (signup-page)
   #_(feed-page request)))
