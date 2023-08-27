(ns keg-party.auth
  (:require [buddy.core.codecs :as codecs]
            [buddy.core.hash :as hash]
            [clojure.string :as str]))

(defn hash-and-salt [password salt]
  (->> (codecs/->bytes password)
       (concat (codecs/->bytes salt))
       byte-array
       hash/sha256
       codecs/bytes->hex))

(defn hash-password [password]
  (let [salt (-> (random-uuid) str codecs/->bytes codecs/bytes->hex)]
    (format "%s:%s" (hash-and-salt password salt) salt)))

(defn check-password [hashed-password attempted-password]
  (let [[password salt] (str/split hashed-password #":")]
    (= (hash-and-salt attempted-password salt) password)))
