(ns parts.datascript.core.core
  (:require
   [clojure.tools.logging :as log]
   [datascript.core :as d]
   [integrant.core :as ig]))

(defmethod ig/init-key ::conn [_ {:keys [schema]}]
  (log/debug "Creating in-memory datascript connection.")
  (d/create-conn schema))
