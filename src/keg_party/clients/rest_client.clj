(ns keg-party.clients.rest-client
  (:require [clojure.pprint :as pp]
            [environ.core :refer [env]]
            [generic.utils :as u]
            [hato.client :as hc]
            [nano-id.core :refer [nano-id]]))

(defn post-tap-data [client-id data]
  (let [host (env :keg-party-host "http://localhost")
        port (env :keg-party-port "3333")
        url  (cond-> host port (str ":" port))]
    (hc/request
     {:url              url
      :method           :post
      :body             (u/to-json-str
                         {:client-id  client-id
                          :message-id (nano-id 10)
                          ;; TODO gzip on both ends
                          :message    (u/base64-encode (with-out-str (pp/pprint data)))})
      :throw-exceptions false})))

(defn tap-in!
  ([] (add-tap (partial post-tap-data (or
                                       (env :keg-party-user-id)
                                       (env :user)
                                       "random user"))))
  ([client-id] (add-tap (partial post-tap-data client-id))))

(defn tap-out! []
  (remove-tap post-tap-data))

(comment
  (tap-in!)
  (tap> "TEST")

  (some?
   (post-tap-data
    (env :user)
    '(let [host (env :keg-party-host "http://localhost")
           port (env :keg-party-port "3333")
           url  (cond-> host port (str ":" port))]
       (hc/request
        {:url    url
         :method :post
         :body   [1 2 3 4]})))))
