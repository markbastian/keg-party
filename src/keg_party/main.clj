(ns keg-party.main
  (:gen-class)
  (:require [keg-party.system :as system]))

(defn run [_]
  (system/start!))

(defn -main
  [& _args]
  (system/start!))
