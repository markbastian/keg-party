(ns keg-party.utils
  (:require
   [clojure.pprint :as pp]
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
