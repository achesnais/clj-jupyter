(ns user
  (:require [clj-jupyter.core :as app]
            [clojure.tools.namespace.repl :refer [refresh]]))

(def app nil)

(def dummy-config
  {:transport        "tcp"
   :ip               "127.0.0.1"
   :hb-port          "4500"
   :stdin-port       "4501"
   :iopub-port       "4502"
   :control-port     "4503"
   :shell-port       "4504"
   :key              (str (java.util.UUID/randomUUID))
   :signature-scheme "hmac-sha256"})

(defn start []
  (alter-var-root #'app (constantly (app/start dummy-config))))

(defn stop []
  (when app
    (deliver (:shutdown-signal app) true)
    (alter-var-root #'app (constantly nil))))

(defn reset []
  (stop)
  (refresh :after 'user/start))
