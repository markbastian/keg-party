(ns keg-party.pages.feed
  (:require
   [keg-party.pages.sidebar :as sidebar]
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
  [{:keys [repo]} username message-id message]
  (let [id            (format "code-block-%s" message-id)
        hljs-code-id  (format "hljs-code-%s" id)
        copy-toast-id (format "copy-toast-%s" id)]
    [:div
     {:id id}
     [:div.collapse.show
      {:id (format "%s-collapse" id)}
      [:div.d-flex.justify-content-between.mb-1
       [:span [:b username]]
       [:span.badge.bg-secondary.align-self-end (format "id:%s" message-id)]]
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
      [:div.col [:hr]]]]))

(defn recent-taps [{{:keys [username]} :session
                    {:keys [limit cursor]
                     :or   {limit 5}}  :params
                    :keys              [repo] :as request}]
  (let [channel-id   (:user/channel_id (repository/user repo {:username username}))
        channel-name (:channel/name (repository/channel repo {:id channel-id}))
        recent-taps  (if cursor
                       (repository/get-recent-channel-taps repo channel-name limit cursor)
                       (repository/get-recent-channel-taps repo channel-name limit))]
    (when (seq recent-taps)
      (concat
       (map
        (fn [{tap-id :tap/id :tap/keys [tap user_id]}]
          (let [u (:user/username (repository/user repo {:id user_id}))]
            (code-block request u tap-id tap)))
        recent-taps)
       [[:div
         {:hx-get     (format "/tap_page?limit=%s&cursor=%s"
                              limit
                              (:tap/id (last recent-taps)))
          :hx-trigger "intersect once"
          :hx-swap    "outerHtml"}
         [:div.text-center
           ;;https://github.com/n3r4zzurr0/svg-spinners
          [:svg
           {:width "24" :height "24" :viewBox "0 0 24 24" :xmlns "http://www.w3.org/2000/svg"}
           [:style ".spinner_OSmW{transform-origin:center;animation:spinner_T6mA .75s step-end infinite}@keyframes spinner_T6mA{8.3%{transform:rotate(30deg)}16.6%{transform:rotate(60deg)}25%{transform:rotate(90deg)}33.3%{transform:rotate(120deg)}41.6%{transform:rotate(150deg)}50%{transform:rotate(180deg)}58.3%{transform:rotate(210deg)}66.6%{transform:rotate(240deg)}75%{transform:rotate(270deg)}83.3%{transform:rotate(300deg)}91.6%{transform:rotate(330deg)}100%{transform:rotate(360deg)}}"]
           [:g.spinner_OSmW
            [:rect {:x "11" :y "1" :width "2" :height "5" :opacity ".14"}]
            [:rect {:x "11" :y "1" :width "2" :height "5" :transform "rotate(30 12 12)" :opacity ".29"}]
            [:rect {:x "11" :y "1" :width "2" :height "5" :transform "rotate(60 12 12)" :opacity ".43"}]
            [:rect {:x "11" :y "1" :width "2" :height "5" :transform "rotate(90 12 12)" :opacity ".57"}]
            [:rect {:x "11" :y "1" :width "2" :height "5" :transform "rotate(120 12 12)" :opacity ".71"}]
            [:rect {:x "11" :y "1" :width "2" :height "5" :transform "rotate(150 12 12)" :opacity ".86"}]
            [:rect {:x "11" :y "1" :width "2" :height "5" :transform "rotate(180 12 12)"}]]]]]]))))

(def channel-create-modal
  [:div#changeChannelModal.modal.fade
   {:tabindex "-1"}
   [:div.modal-dialog
    [:div.modal-content
     [:div.modal-header
      [:h1.modal-title.fs-5 "Change Channel"]
      [:button.btn-close
       {:type            "button"
        :data-bs-dismiss "modal"}]]
     [:form
      {:ws-send "true"
       :hx-vals (u/to-json-str {:command :change-channel})
       :method  :post}
      [:div.modal-body
       [:div.form-group
        [:label.col-form-label "Destination channel:"]
        [:input.form-control
         {:type         "text"
          :name         "channel-name"
          :autocomplete "off"}]]]
      [:div.modal-footer
       [:button.btn.btn-secondary
        {:type            "button"
         :data-bs-dismiss "modal"}
        "Close"]
       [:button.btn.btn-primary
        {:type            "button"
         :data-bs-dismiss "modal"
         :ws-send         "true"
         :hx-vals         (u/to-json-str {:command :change-channel})
         :method          :post}
        "Go"]]]]]])

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
        :hx-vals (u/to-json-str {:command :delete-unstarred-taps})}
       "Delete Unstarred"]]
     [:li.nav-item
      [:a.nav-link.active
       {:href           "#"
        :data-bs-toggle "modal"
        :data-bs-target "#changeChannelModal"} "Change Channel"]]
     [:li.nav-item
      [:a.nav-link {:href "/logout"}
       (format "Logout %s" (:username session))]]]]])

(defn notifications-pane [& r]
  (into [:div#tap-log.taplog.p-2] r))

(defn feed-page [request]
  [:div
   {:hx-ext     "ws"
    :ws-connect (format "/ws")}
   channel-create-modal
   (navbar request)
   (sidebar/sidebar request)
   (notifications-pane
    (recent-taps request))])
