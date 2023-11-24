(ns keg-party.system
  (:require
   [keg-party.migrations :as migrations]
   [keg-party.repository :as repository-api]
   [keg-party.repository.sql-repository :as sql-repo]
   [keg-party.web :as web]
   [clojure.tools.logging :as log]
   [environ.core :refer [env]]
   [generic.client-api :as client-api]
   [generic.ws-handlers :as ws-handlers]
   [integrant.core :as ig]
   [parts.next.jdbc.core :as jdbc]
   [parts.ring.adapter.jetty9.core :as jetty9]
   [parts.ws-handler :as ws]))

(defmethod ig/init-key ::repository [_ {:keys [ds]}]
  (log/debug "Creating repository")
  (sql-repo/instance ds))

(defmethod ig/init-key ::client-manager [_ _]
  (log/debug "Creating client manager")
  (client-api/atomic-client-manager))

(def config
  {::jdbc/datasource {:dbtype "sqlite"
                      :dbname "keg-party.db"}
   ::jdbc/migrations {:db         (ig/ref ::jdbc/datasource)
                      :migrations migrations/migrations}
   ::repository      {:ds (ig/ref ::jdbc/datasource)}
   ::client-manager  {}
   ::ws/ws-handlers  {:on-connect #'ws-handlers/on-connect
                      :on-text    #'ws-handlers/on-text
                      :on-close   #'ws-handlers/on-close
                      :on-error   #'ws-handlers/on-error}
   ::jetty9/server   {:host           "0.0.0.0"
                      :port           (parse-long (env :keg-party-port "3333"))
                      :join?          false
                      :client-manager (ig/ref ::client-manager)
                      :repo           (ig/ref ::repository)
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

  (let [repo (::repository (system))]
    (repository-api/users repo)
    ;(repository-api/user repo {:username "mbastian"})
    )

  (let [cm (::client-manager (system))]
    (client-api/clients cm)))
