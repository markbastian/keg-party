(ns keg-party.pages.signup)

(defn signup-page [_request]
  [:div {:id    "app"
         :style "position:absolute; top:20%; right:0; left:0;"}
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
      {:id           "enter-password"
       :name         "password"
       :type         "password"
       :placeholder  "Enter a really great password"
       :autocomplete "off"
       :onkeyup "checkSamePassword()"}]
     [:label "Confirm password"]
     [:input.form-control
      {:id           "confirm-password"
       :name         "confirm-password"
       :type         "password"
       :placeholder  "Re-enter that same password"
       :autocomplete "off"
       :onkeyup "checkSamePassword()"}]
     [:label {:id "message"}]]
    [:div.d-grid.gap-2.p-2
     [:button.btn.btn-primary.btn-dark
      {:id "password-submit-button"
       :disabled true
       :type "submit"}
      "Sign up"]]]])
