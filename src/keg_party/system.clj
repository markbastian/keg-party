(ns keg-party.system
  (:require [keg-party.commands :as commands]
            [keg-party.web :as web]
            [keg-party.ws-handlers :as ws-handlers]
            [clojure.pprint :as pp]
            [clojure.tools.logging :as log]
            [environ.core :refer [env]]
            [integrant.core :as ig]
            [parts.datascript.core.core :as ds]
            [parts.ring.adapter.jetty9.core :as jetty9]
            [parts.state :as ps]
            [parts.ws-handler :as ws]))

;; TODO - Add stack trace to this and use with reporting in UI
(defn tap-echo [context data]
  (commands/dispatch-command
   context
   {:command      :chat-message
    :chat-message (with-out-str (pp/pprint data))}))

(defmethod ig/init-key ::tap [_ config]
  (log/debug "Launching tap> component")
  (let [tap-fn (partial tap-echo config)]
    (add-tap tap-fn)
    tap-fn))

(defmethod ig/halt-key! ::tap [_ tap-fn]
  (log/debug "Removing tap> component")
  (remove-tap tap-fn))

(def chat-schema
  {:username  {:db/unique :db.unique/identity}
   :room-name {:db/unique :db.unique/identity}
   :user      {:db/valueType   :db.type/ref
               :db/cardinality :db.cardinality/one}
   :room      {:db/valueType   :db.type/ref
               :db/cardinality :db.cardinality/one}})

(def config
  {[::chat-state ::ds/conn]    {:schema chat-schema}
   [::clients-state ::ps/atom] {}
   ::ws/ws-handlers            {:on-connect #'ws-handlers/on-connect
                                :on-text    #'ws-handlers/on-text
                                :on-close   #'ws-handlers/on-close
                                :on-error   #'ws-handlers/on-error}
   ::tap                       {:clients (ig/ref [::clients-state ::ps/atom])
                                :conn    (ig/ref [::chat-state ::ds/conn])}
   ::jetty9/server             {:host        "0.0.0.0"
                                :port        (parse-long (env :keg-party-port "3000"))
                                :join?       false
                                :clients     (ig/ref [::clients-state ::ps/atom])
                                :conn        (ig/ref [::chat-state ::ds/conn])
                                :ws-handlers (ig/ref ::ws/ws-handlers)
                                :handler     #'web/handler}})

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
  (system))
