(ns keg-party.utils
  (:require
   [clojure.pprint :as pp]
   [clojure.string :as str]
   [environ.core :refer [env]]
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

(defn stack-dump
  ([] (stack-dump {:includes-regex (:keg-party-includes-regex env)
                   :excludes-regex (:keg-party-excludes-regex env "java.*|clojure.*|nrepl.*")}))
  ([{:keys [includes-regex excludes-regex]}]
   (let [include (if (seq includes-regex)
                   (partial re-matches (re-pattern includes-regex))
                   (constantly true))
         exclude (if (seq excludes-regex)
                   (partial re-matches (re-pattern excludes-regex))
                   (constantly false))]
     (for [ste (.getStackTrace (Thread/currentThread))
           :let [{:keys [className fileName lineNumber]} (bean ste)
                 class-name (str/replace className "_" "-")]
           :when (and (include class-name) (not (exclude class-name)))
           :let [[n f] (str/split class-name #"\$")]]
       (merge
        {:file fileName :line lineNumber}
        (if f
          {:ns (symbol (str/replace n "_" "-")) :fn (symbol f)}
          {:class className}))))))

(comment
  (stack-dump)
  (stack-dump {:excludes-regex "java.*|clojure.*|nrepl.*"})
  (stack-dump {:includes-regex "keg-party.*"}))
