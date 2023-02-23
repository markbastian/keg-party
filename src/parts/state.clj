(ns parts.state
  (:require
   [clojure.tools.logging :as log]
   [integrant.core :as ig]))

(defmethod ig/init-key ::atom [_ initial-value]
  (log/debug "Creating atom")
  (atom initial-value))

(defmethod ig/init-key ::ref [_ initial-value]
  (log/debug "Creating ref")
  (ref initial-value))
