(ns keg-party.pages
  "Functions to creat server rendered pages."
  (:require
   [keg-party.repository :as repository]
   [clojure.pprint :as pp]
   [clojure.string :as str]
   [generic.client-api :as client-api]
   [generic.utils :as u]
   [hiccup.page :refer [html5 include-css include-js]]
   [ring.util.codec :as codec]))

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
  ([{:keys [repo]} username message-id message last?]
   (let [id            (format "code-block-%s" message-id)
         hljs-code-id  (format "hljs-code-%s" id)
         copy-toast-id (format "copy-toast-%s" id)]
     [:div
      (cond->
       {:id id}
        last?
        (merge {:hx-get     (format "/tap_page?limit=3&cursor=%s" message-id)
                :hx-trigger "intersect once"
                :hx-swap    "afterend"}))
      [:div.collapse.show
       {:id (format "%s-collapse" id)}
       [:span username]
       [:div.d-flex.justify-content-between.align-items-top
        [:div.overflow-auto
         [:pre
          [:code.language-clojure
           {:id hljs-code-id}
           message]
          [:script (format
                    "hljs.highlightElement(document.getElementById('%s'))"
                    hljs-code-id)]]]
        [:div.d-flex.flex-column.gap-1
         [:div
          [:button.btn.btn-dark.btn-sm
           {:onclick (format
                      "navigator.clipboard.writeText(atob('%s'));
                            showToast('%s')"
                      (u/base64-encode message)
                      copy-toast-id)}
           [:i.fa-solid.fa-copy]]
          [:div.position-fixed.bottom-0.end-0.p-3.w-25
           [:div.toast {:id copy-toast-id :role "alert"}
            [:div.toast-body "Copied!"]]]]
         [:a {:href (format "tap/%s" message-id)}
          [:button.btn.btn-dark.btn-sm
           [:i.fa-solid.fa-bore-hole]]]
         [:button.btn.btn-dark.btn-sm
          {:ws-send "true"
           :hx-vals (u/to-json-str {:command    :delete-message
                                    :message-id message-id})}
          [:i.fa-solid.fa-trash]]
         (let [favorite? (repository/get-favorite repo {:username username
                                                        :tap-id   message-id})]
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
      [:a.nav-link.active
       {:href    "#"
        :ws-send "true"
        :hx-vals (u/to-json-str {:command :delete-unstarred-taps})} "Delete Unstarred"]]
     [:li.nav-item
      [:a.nav-link {:href "/logout"}
       (format "Logout %s" (:username session))]]]]])

(defn recent-taps [{{:keys [username]} :session
                    :keys              [repo] :as request}]
  (let [recent-taps (repository/get-recent-taps repo username 5)
        tap-count   (dec (count recent-taps))]
    (map-indexed
     (fn [idx {:tap/keys [tap id]}]
       (code-block request username id tap (= idx tap-count)))
     recent-taps)))

(defn feed-page [request]
  [:div
   {:hx-ext     "ws"
    :ws-connect (format "/ws")}
   (navbar request)
   (notifications-pane
    (recent-taps request))])

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

(defn detail-code-block [{tap-id :tap/id tap-data :tap/data :as _tap}
                         path
                         selected]
  (let [final-data   (cond-> (get-in tap-data path)
                       (seq selected)
                       (select-keys selected))
        hljs-code-id (format "tap-detail-%s" tap-id)]
    [:pre
     {:id (format "pre-%s" hljs-code-id)}
     [:code.language-clojure
      {:id hljs-code-id}
      (with-out-str
        (pp/pprint
         final-data))]
     [:script (format
               "hljs.highlightElement(document.getElementById('%s'))"
               hljs-code-id)]]))

(defn build-drill-url [base-drill-url drill-path]
  (->> drill-path
       (map (fn [v]
              (codec/url-encode
               (cond->> v
                 (string? v)
                 (format "\"%s\"")))))
       (str/join "/")
       (format "%s/%s" base-drill-url)))

(defn data-subselect-form [{tap-data :tap/data tap-id :tap/id}
                           path]
  (let [drilled-data (get-in tap-data path)
        hljs-code-id (format "tap-detail-%s" tap-id)
        drill-url    (format "/tap/%s" tap-id)]
    (cond
      (indexed? drilled-data)
      [:div.card-body.d-flex.justify-content-center
       [:div.dropdown
        [:button.btn.btn-secondary.dropdown-toggle.btn-sm.mx-auto
         {:type           "button"
          :data-bs-toggle "dropdown"}
         "Select Row"]
        [:ul.dropdown-menu
         (for [idx (range (count drilled-data))]
           [:li
            [:a.dropdown-item
             {:href (build-drill-url drill-url (conj path idx))}
             idx]])]]]
      (associative? drilled-data)
      (let [data-keys (sort (keys drilled-data))]
        [:div.card-body
         [:form
          (for [data-key data-keys]
            [:div.form-group.p-1
             [:input
              {:type      "checkbox"
               :name      (str data-key)
               :checked   "true"
               :hx-event  "onchange"
               :hx-post   (build-drill-url drill-url path)
               :hx-swap   "outerHTML"
               :hx-target (format "#pre-%s" hljs-code-id)}]
             [:label
              [:a
               {:href (build-drill-url drill-url (conj path data-key))}
               (str data-key)]]])]]))))

(defn breadcrumbs [{tap-id :tap/id} drill-path]
  (let [drill-url (format "/tap/%s" tap-id)]
    [:nav.p-2
     {:style "--bs-breadcrumb-divider: '>';"}
     [:ol.breadcrumb
      [:li.breadcrumb-item [:a {:href "/feed"} "feed"]]
      (let [paths (reductions (fn [acc p] (conj acc p)) [] drill-path)]
        (map-indexed
         (fn [idx path]
           (let [label (str (or (peek path) "/"))]
             (if (< idx (dec (count paths)))
               [:li.breadcrumb-item
                [:a {:href (build-drill-url drill-url path)}
                 label]]
               [:li.breadcrumb-item.active label])))
         paths))]]))

(defn details-navbar [{:keys [session]}]
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
      [:a.nav-link.active {:href "/feed"} "Feed"]]
     [:li.nav-item
      [:a.nav-link.active {:href "/clients"} "Clients"]]
     [:li.nav-item
      [:a.nav-link {:href "/logout"}
       (format "Logout %s" (:username session))]]]]])

(defn tap-detail-page [request tap drill-path]
  [:div {:id "tap-detail-page"}
   (details-navbar request)
   (breadcrumbs tap drill-path)
   (let [code-block (detail-code-block tap drill-path nil)]
     [:div.d-flex.flex-row
      (when-some [form (data-subselect-form tap drill-path)]
        [:div.card.p-2 form])
      [:div.overflow-auto.flex-grow-1.p-2
       code-block]
      [:div.justify-content-end.p-2
       [:button.btn.btn-dark.btn-sm
        {:onclick (format
                   "let text = document.getElementById('%s').textContent;
                    navigator.clipboard.writeText(text);
                    showToast('%s');"
                   (format "tap-detail-%s" (:tap/id tap))
                   "tap-detail-copy-toast-id")}
        [:i.fa-solid.fa-copy]]
       [:div.position-fixed.bottom-0.end-0.p-3.w-25
        [:div.toast {:id "tap-detail-copy-toast-id" :role "alert"}
         [:div.toast-body "Copied!"]]]]])])

(defn wrap-as-page [content]
  (html5
   [:head
    [:base {:href "/"}]
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
     "public/keg_party/highlight.min.js"
     "public/keg_party/showtoast.js")
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]]
   content))

(defn landing-page-html [_request]
  (wrap-as-page
   (signup-page)
   #_(feed-page request)))
