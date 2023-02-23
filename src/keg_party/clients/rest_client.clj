(ns keg-party.clients.rest-client
  (:require [keg-party.utils :as u]
            [clojure.pprint :as pp]
            [environ.core :refer [env]]
            [hato.client :as hc]))

(defn post-tap-data [data]
  (let [host (env :keg-party-host "http://localhost")
        port (env :keg-party-port "3000")
        url  (cond-> host port (str ":" port))]
    (hc/request
     {:url              url
      :method           :post
      :body             (u/base64-encode (with-out-str (pp/pprint data)))
      :throw-exceptions false})))

(defn tap-in! []
  (add-tap post-tap-data))

(defn tap-out! []
  (remove-tap post-tap-data))

(comment
  (post-tap-data
   '(let [host (env :keg-party-host "http://localhost")
          port (env :keg-party-port "3000")
          url  (cond-> host port (str ":" port))]
      (hc/request
       {:url    url
        :method :post
        :body   [1 2 3 4]}))))
