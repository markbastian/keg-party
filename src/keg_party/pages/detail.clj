(ns keg-party.pages.detail
  (:require [clojure.pprint :as pp]
            [clojure.string :as str]
            [ring.util.codec :as codec]))

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
