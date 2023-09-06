(ns keg-party.pages.clients
  (:require [generic.client-api :as client-api]))

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
