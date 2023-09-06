(ns keg-party.pages.login)

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
