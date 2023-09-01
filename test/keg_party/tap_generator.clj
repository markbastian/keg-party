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
    (tap> a))

  ;; A code form
  (tap>
   '(defn start!
      ([config]
       (alter-var-root #'*system* (fn [s] (if-not s (ig/init config) s))))
      ([] (start! config))))

  ;; A matrix
  (tap> (vec (repeatedly 10 #(vec (repeatedly 10 (fn [] (rand-int 10)))))))

  ;; A tall tap
  (tap> (into {} (for [i (range 100)]
                   [(keyword (str "key-" i)) i])))

  ;; A wide tap
  (tap> (apply str (repeat 1000 "X"))))
