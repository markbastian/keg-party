(ns parts.core.async
  (:require
   [clojure.core.async :as async]
   [clojure.tools.logging :as log]
   [integrant.core :as ig]))

(defmethod ig/init-key ::channel [_ _]
  (log/debug "Creating a channel")
  (async/chan))
