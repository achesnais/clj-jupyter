(ns clj-jupyter.heartbeat
  (:require [taoensso.timbre :as log])
  (:import zmq.ZMQ))

(set! *warn-on-reflection* true)

(defn start [{:keys [transport ip hb-port]} shutdown-signal]
  (future
    (log/info "Starting hearbeat...")
    (let [ctx       (ZMQ/createContext)
          hb-socket (ZMQ/socket ctx ZMQ/ZMQ_REP)]
      (ZMQ/bind hb-socket (str transport "://" ip ":" hb-port))
      (ZMQ/setSocketOption hb-socket ZMQ/ZMQ_RCVTIMEO (int 250))
      (log/debug "Heartbeat started!")
      (while (not (realized? shutdown-signal))
        (when-let [msg (ZMQ/recv hb-socket 0)]
          (ZMQ/send hb-socket msg ZMQ/ZMQ_DONTWAIT)))
      (log/debug "Stopping heartbeat...")
      (ZMQ/close hb-socket)
      (ZMQ/term ctx)
      (log/info "Hearbeat stopped!")
      :shutdown-success)))
