(ns keg-party.utils
  (:require
   [clojure.pprint :as pp]
   [clojure.string :as str]
   [jsonista.core :as j])
  (:import (java.util Base64)))

(defn to-json-str [m]
  (j/write-value-as-string m j/keyword-keys-object-mapper))

(defn read-json [str]
  (j/read-value str j/keyword-keys-object-mapper))

(defn print-params [request]
  (pp/pprint
   (select-keys
    request
    [:params :parameters :form-params :path-params :query-params])))

(defn base64-encode [s]
  (let [s (if (string? s) s (pr-str s))]
    (.encodeToString (Base64/getEncoder) (.getBytes s))))

(defn base64-decode [^String s]
  (String. (.decode (Base64/getDecoder) s)))

(defn stack-dump []
  (for [ste (.getStackTrace (Thread/currentThread))
        :let [{:keys [className fileName lineNumber]} (bean ste)]
        ;:when (str/includes? className "keg_party")
        :let [[n f] (str/split className #"\$")]]
    {:ns (symbol (str/replace n "_" "-"))
     :fn (symbol f)
     :file fileName
     :line lineNumber}))
