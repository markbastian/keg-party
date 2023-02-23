(ns keg-party.queries
  (:require
   [datascript.core :as d]))

(defn current-room-name [db username]
  (some-> db (d/entity [:username username]) :room :room-name))
