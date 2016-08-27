(ns clj-jupyter.core
  (:require [clj-jupyter
             [heartbeat :as heartbeat]
             [shell :as shell]
             [util :as util]]
            [clojure.data.json :as json]
            [taoensso.timbre :as log])
  (:import zmq.ZMQ)
  (:gen-class))

(set! *warn-on-reflection* true)

(defn start [config]
  (let [shutdown-signal (promise)]
    {:shutdown-signal shutdown-signal
     :heartbeat       (heartbeat/start config shutdown-signal)
     :shell           (shell/start config shutdown-signal)}))

(defn -main [config-path]
  (let [config (util/json->edn (slurp config-path))]
    (log/info (str "Starting app with config: " config))
    (start config)))
