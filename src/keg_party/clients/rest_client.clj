(ns keg-party.clients.rest-client
  (:require [clojure.pprint :as pp]
            [environ.core :refer [env]]
            [generic.utils :as u]
            [hato.client :as hc]))

(defn post-tap-data
  ([{:keys [username password]
     :or   {username (or (env :keg-party-user-id) (env :user))
            password (env :keg-party-password)}}
    data]
   {:pre [username password]}
   (let [host (env :keg-party-host "http://localhost")
         port (env :keg-party-port "3333")
         url  (cond-> host port (str ":" port))]
     (hc/request
      {:url              url
       :method           :post
       :body             (u/base64-encode (with-out-str (pp/pprint data)))
       :basic-auth       {:user username
                          :pass password}
       :throw-exceptions false})))
  ([data] (post-tap-data {} data)))

(defn tap-in!
  ([] (add-tap post-tap-data))
  ([username password] (add-tap (partial post-tap-data {:username username :password password}))))

(defn tap-out! []
  (remove-tap post-tap-data))

(comment
  (tap-in!)

  (post-tap-data
   '(let [host (env :keg-party-host "http://localhost")
          port (env :keg-party-port "3333")
          url  (cond-> host port (str ":" port))]
      (hc/request
       {:url    url
        :method :post
        :body   [1 2 3 4]}))))
