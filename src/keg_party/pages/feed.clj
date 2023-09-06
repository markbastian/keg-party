(ns keg-party.pages.feed
  (:require
   [keg-party.repository :as repository]
   [generic.utils :as u]))

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
