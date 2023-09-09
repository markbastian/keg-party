(ns buildclient
  "See https://clojure.org/guides/tools_build for reference."
  (:require
   [clojure.tools.build.api :as b]))

(def lib 'com.github.markbastian/keg-party-client)
(def version (format "0.0.%s" (b/git-count-revs nil)))
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn" :aliases [:client]}))
(def jar-file (format "target/%s.jar" (name lib)))
(def uberjar-file (format "target/%s-standalone.jar" (name lib)))

(defn clean [_]
  (b/delete {:path "target"}))

(defn jar
  "Build a jarfile. Invoke with `clj -T:build-client jar`"
  [_]
  (clean nil)
  (b/copy-dir {:src-dirs   ["client/src"]
               :target-dir class-dir})
  (b/write-pom {:class-dir class-dir
                :lib       lib
                :version   version
                :basis     basis
                :src-dirs  ["client/src"]})
  (b/copy-file
   {:src (b/pom-path {:class-dir class-dir
                      :lib       lib
                      :version   version
                      :basis     basis
                      :src-dirs  ["client/src"]})
    :target "pom.xml"})
  (b/jar {:class-dir class-dir
          :jar-file  jar-file}))
