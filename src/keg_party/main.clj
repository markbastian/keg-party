(ns keg-party.main
  (:gen-class)
  (:require [keg-party.system :as system]
            [clojure.java.browse :refer [browse-url]]
            [environ.core :refer [env]]))

(defn run [_]
  (system/start!)
  ())

(defn -main
  [& _args]
  (system/start!)
  (let [host (env :keg-party-host "http://localhost")
        port (env :keg-party-port "3333")
        url  (cond-> host port (str ":" port))]
    (browse-url url)))
