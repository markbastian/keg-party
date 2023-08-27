(ns keg-party.system
  (:require [keg-party.migrations :as migrations]
            [keg-party.web :as web]
            [environ.core :refer [env]]
            [generic.client-api :as client-api]
            [generic.ws-handlers :as ws-handlers]
            [integrant.core :as ig]
            [parts.next.jdbc.core :as jdbc]
            [parts.ring.adapter.jetty9.core :as jetty9]
            [parts.ws-handler :as ws]))

(def config
  {::jdbc/datasource {:dbtype "sqlite"
                      :dbname "keg-party.db"}
   ::jdbc/migrations {:db         (ig/ref ::jdbc/datasource)
                      :migrations migrations/migrations}
   ::ws/ws-handlers  {:on-connect #'ws-handlers/on-connect
                      :on-text    #'ws-handlers/on-text
                      :on-close   #'ws-handlers/on-close
                      :on-error   #'ws-handlers/on-error}
   ::jetty9/server   {:host           "0.0.0.0"
                      :port           (parse-long (env :keg-party-port "3333"))
                      :join?          false
                      :ds             (ig/ref ::jdbc/datasource)
                      :client-manager (client-api/atomic-client-manager)
                      :ws-handlers    (ig/ref ::ws/ws-handlers)
                      :handler        #'web/handler}})

(defonce ^:dynamic *system* nil)

(defn system [] *system*)

(defn start!
  ([config]
   (alter-var-root #'*system* (fn [s] (if-not s (ig/init config) s))))
  ([] (start! config)))

(defn stop! []
  (alter-var-root #'*system* (fn [s] (when s (ig/halt! s) nil))))

(defn restart!
  ([config] (stop!) (start! config))
  ([] (restart! config)))

(comment
  (start!)
  (stop!)
  (restart!)
  (system)

  (require '[keg-party.clients.rest-client])
  (keg-party.clients.rest-client/tap-in!)

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
