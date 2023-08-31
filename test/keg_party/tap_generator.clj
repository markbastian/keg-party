(ns keg-party.tap-generator
  (:require
   ;[clojure.test :refer :all]
   [malli.core :as m]
   [malli.generator :as mg]))

(def address
  (m/schema
   [:map
    [:id string?]
    [:tags [:set keyword?]]
    [:address
     [:map
      [:street string?]
      [:city string?]
      [:zip int?]
      [:lonlat [:tuple double? double?]]]]]))

(comment
  (doseq [a (mg/sample address)]
    (tap> a)))
