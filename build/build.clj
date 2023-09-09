(ns build
  "See https://clojure.org/guides/tools_build for reference."
  (:require
   [clojure.tools.build.api :as b]))

(def lib 'com.github.markbastian/keg-party)
(def version (format "0.0.%s" (b/git-count-revs nil)))
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn" :aliases [:server]}))
(def jar-file (format "target/%s.jar" (name lib)))
(def uberjar-file (format "target/%s-standalone.jar" (name lib)))

(defn clean [_]
  (b/delete {:path "target"}))

(defn compile-java [_]
  (b/javac {:src-dirs ["java"]
            :class-dir class-dir
            :basis basis
            :javac-opts ["--release" "11"]}))

(defn jar
  "Build a jarfile. Invoke with `clj -T:build jar`"
  [_]
  (compile-java nil)
  (b/write-pom {:class-dir class-dir
                :lib lib
                :version version
                :basis basis
                :src-dirs ["src"]})
  (b/copy-dir {:src-dirs ["src" "resources"]
               :target-dir class-dir})
  (b/jar {:class-dir class-dir
          :jar-file jar-file}))

(defn uber
  "Build an executable uberjar. Invoke with `clj -T:build uber`"
  [_]
  (clean nil)
  (b/copy-dir {:src-dirs ["src" "resources"]
               :target-dir class-dir})
  (b/compile-clj {:basis basis
                  :src-dirs ["src"]
                  :class-dir class-dir})
  (b/write-pom {:class-dir class-dir
                :lib lib
                :version version
                :basis basis
                :src-dirs ["src"]})
  (b/copy-file
   {:src (b/pom-path {:class-dir class-dir
                      :lib       lib
                      :version   version
                      :basis     basis
                      :src-dirs  ["src"]})
    :target "pom.xml"})
  (b/uber {:class-dir class-dir
           :uber-file uberjar-file
           :basis basis
           :main 'keg-party.main}))
