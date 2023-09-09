(ns keg-party.clients.rest-client
  (:require [clojure.pprint :as pp]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [environ.core :refer [env]]
            [hato.client :as hc])
  (:import (java.util Base64)))

(defn base64-encode [s]
  (let [s (if (string? s) s (pr-str s))]
    (.encodeToString (Base64/getEncoder) (.getBytes s))))

(defn post-tap-data
  ([{:keys [username password]
     :or   {username (or (env :keg-party-username) (env :user))
            password (env :keg-party-password)}}
    data]
   {:pre [username password]}
   (let [host    (env :keg-party-host "http://localhost")
         port    (env :keg-party-port "3333")
         url     (cond-> host port (str ":" port))
         body    (base64-encode (with-out-str (pp/pprint data)))
         request {:url              url
                  :method           :post
                  :body             body
                  :basic-auth       {:user username
                                     :pass password}
                  :throw-exceptions false}
         status (:status (hc/request request))]
     (case status
       200 (log/trace "Data posted to tap server.")
       401 (log/warnf
            (str/join
             ["You are were unable to post to the tap server as %s. "
              "Is the KEG_PARTY_USERNAME set correctly?"])
            username)
       403 (log/warn
            (str/join
             ["You are unauthorized to post to the tap server. "
              "Is the KEG_PARTY_PASSWORD set correctly?"]))
       (log/warnf "tap-server returned status code %s" status))))
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
