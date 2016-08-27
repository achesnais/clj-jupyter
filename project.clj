(defproject clj-jupyter "0.1.0"
  :description "A Clojure kernel for Jupyter Notebooks."
  :url "https://github.com/achesnais/clj-jupyter"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[com.taoensso/timbre "4.7.0"]
                 [org.clojure/clojure "1.8.0"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/tools.nrepl "0.2.12"]
                 [org.zeromq/jeromq "0.3.5"]]
  :main clj-jupyter.core
  :repl-options {:init-ns user}
  :profiles
  {:dev     {:source-paths ["dev"]
             :dependencies [[org.clojure/tools.namespace "0.2.11"
                             :exclude [org.clojure/clojure]]]}
   :uberjar {:aot :all}}
  :uberjar-name "IClojure.jar")
