(ns keg-party.pages.signup)

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
