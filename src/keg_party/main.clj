(ns keg-party.main
  (:require [keg-party.system :as system]))

(defn run [_]
  (system/start!))
