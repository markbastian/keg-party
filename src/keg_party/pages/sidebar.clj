(ns keg-party.pages.sidebar
  (:require
   [keg-party.repository :as repository]
   [generic.utils :as u]))

(defn user-bullet [{:user/keys [username]}
                   & attrs]
  [:li
   (into {:id (format (format "user-bullet-%s" username))} attrs)
   [:a.rounded
    {:href        "#"
     :draggable   "true"
     :ondragstart "console.log('dragging');"
     :class       "link-light"}
    username]])

(defn channel-list [{:keys [repo]}
                    {channel-name :channel/name channel-id :channel/id}
                    & attrs]
  [:li.mb-1
   [:button.btn.btn-toggle.align-items-center.rounded.collapsed
    {:data-bs-toggle "collapse"
     :data-bs-target (format "#channel-%s-list" channel-id)}
    [:span
     channel-name
     [:a {:href    "#"
          :ws-send "true"
          :hx-vals (u/to-json-str {:command      :change-channel
                                   :channel-name channel-name})
          :method  :post}
      [:i.fa-solid.fa-right-to-bracket.m-1]]
     (when-not (= channel-name "public")
       [:a {:href    "#"
            :ws-send "true"
            :hx-vals (u/to-json-str {:command      :delete-channel
                                     :channel-name channel-name})
            :method  :post}
        [:i.fa-solid.fa-trash.m-1]])]]
   [:ul.btn-toggle-nav.list-unstyled.fw-normal.pb-1.small
    (into {:id (format "channel-%s-list" channel-id)} attrs)
    (for [user (repository/get-channel-users repo {:name channel-name})]
      (user-bullet user))]])

(defn channels-list [{:keys [repo] :as request} & attrs]
  [:ul#sidebar-channel-list.btn-toggle-nav.list-unstyled.fw-normal.pb-1.small
   (into {} attrs)
   (for [channel (repository/channels repo)]
     (channel-list request channel))])

(defn sidebar [request]
  [:div#tap-sidebar.sidebar
   [:button#toggle-sidebar.btn-collapse {:onclick "toggleSidebar()"}
    [:i.fa-solid.fa-angles-left]]
   [:div.m-2
    [:span.text-light "# Channels"
     [:button.btn.text-light
      {:href           "#"
       :data-bs-toggle "modal"
       :data-bs-target "#changeChannelModal"}
      [:i.fa-solid.fa-plus.m-1]]]
    (channels-list request)]])
