(ns build
  "See https://clojure.org/guides/tools_build for reference."
  (:require
   [clojure.tools.build.api :as b]))

(def version (format "0.0.%s" (b/git-count-revs nil)))
(def class-dir "target/classes")

(defn clean [_]
  (b/delete {:path "target"}))

(defn server-uberjar
  "Build the keg-party server executable uberjar. Invoke with `clj -T:build server-uberjar`"
  [_]
  (let [lib 'com.github.markbastian/keg-party
        uberjar-file (format "target/%s-standalone.jar" (name lib))
        basis (b/create-basis {:project "deps.edn" :aliases [:server]})]
    (clean nil)
    (b/copy-dir {:src-dirs   ["src" "resources"]
                 :target-dir class-dir})
    (b/compile-clj {:basis     basis
                    :src-dirs  ["src"]
                    :class-dir class-dir})
    (b/write-pom {:class-dir class-dir
                  :lib       lib
                  :version   version
                  :basis     basis
                  :src-dirs  ["src"]})
    (b/copy-file
     {:src    (b/pom-path {:class-dir class-dir
                           :lib       lib
                           :version   version
                           :basis     basis
                           :src-dirs  ["src"]})
      :target "pom.xml"})
    (b/uber {:class-dir class-dir
             :uber-file uberjar-file
             :basis     basis
             :main      'keg-party.main})))

(defn client-jar
  "Build the keg-party client jarfile. Invoke with `clj -T:build client-jar`"
  [_]
  (let [lib 'com.github.markbastian/keg-party-client
        jar-file (format "target/%s.jar" (name lib))
        basis (b/create-basis {:project "deps.edn" :aliases [:client]})]
    (clean nil)
    (b/copy-dir {:src-dirs   ["client/src"]
                 :target-dir class-dir})
    (b/write-pom {:class-dir class-dir
                  :lib       lib
                  :version   version
                  :basis     basis
                  :src-dirs  ["client/src"]})
    (b/copy-file
     {:src    (b/pom-path {:class-dir class-dir
                           :lib       lib
                           :version   version
                           :basis     basis
                           :src-dirs  ["client/src"]})
      :target "pom.xml"})
    (b/jar {:class-dir class-dir
            :jar-file  jar-file})))
