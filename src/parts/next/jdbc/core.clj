(ns parts.next.jdbc.core
  (:require
   [clojure.tools.logging :as log]
   [integrant.core :as ig]
   [next.jdbc :as jdbc]))

(defmethod ig/init-key ::datasource [_ opts]
  (log/debug "Getting jdbc datasource.")
  (jdbc/get-datasource opts))

(defmethod ig/init-key ::migrations [_ {:keys [db migrations]}]
  (log/debug "Running migrations...")
  (jdbc/with-transaction [tx db]
    (doseq [migration migrations]
      (jdbc/execute! tx migration))))

(defmethod ig/init-key ::teardown [_ m] m)

(defmethod ig/halt-key! ::teardown [_ {:keys [db commands]}]
  (log/debug "Running SQL teardown...")
  (jdbc/with-transaction [tx db]
    (doseq [command commands]
      (jdbc/execute! tx command))))
