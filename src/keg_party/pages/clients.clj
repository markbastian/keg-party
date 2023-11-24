(ns keg-party.pages.clients
  (:require [generic.client-api :as client-api]))

(defn clients-page [{{:keys [_session-id] :as session} :session :keys [client-manager]}]
  [:div
   (into
    [:table.table.table-striped.table-dark.table-bordered.table-sm
     [:tr
      [:th "Username"]
      [:th "Session ID"]
      [:th "Protocol"]]]
    (for [{:keys [session-id username ws]} (client-api/clients client-manager)]
      [:tr
       [:td (cond-> username (= (:username session) username) (str " *"))]
       [:td (cond-> session-id (= session-id session-id) (str " *"))]
       [:td (if (some? ws) "ws" "?")]]))
   [:p [:a {:href "/feed"} "Feed"]]])
