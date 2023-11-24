(ns keg-party.pages.sidebar
  (:require [keg-party.repository :as repository]))

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

(defn user-bullets [{:keys [repo] :as _request}]
  (for [user (repository/users repo)]
    (user-bullet user)))

(defn channel-list [{:keys [repo]}
                    {channel-name :channel/name channel-id :channel/id}
                    & attrs]
  [:li.mb-1
   [:button.btn.btn-toggle.align-items-center.rounded.collapsed
    {:data-bs-toggle "collapse"
     :data-bs-target (format "#channel-%s-list" channel-id)}
    channel-name]
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
   (channels-list request)])
