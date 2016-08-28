(ns clj-jupyter.shell
  (:require [clj-jupyter.util :as util]
            [clojure.pprint :as pprint]
            [clojure.tools.nrepl :as nrepl]
            [clojure.tools.nrepl.server :as nrepl.server]
            [taoensso.timbre :as log])
  (:import clojure.tools.nrepl.transport.FnTransport
           javax.crypto.Mac
           javax.crypto.spec.SecretKeySpec
           [zmq Msg SocketBase ZMQ]))

(set! *warn-on-reflection* true)

(defprotocol ILifecycle
  (init [this])
  (close [this]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Security

(defn signer-fn [^String key]
  (let [hmac-sha256 (Mac/getInstance "HmacSHA256")
        key         (SecretKeySpec. (.getBytes key) "HmacSHA256")]
    (.init hmac-sha256 key)
    (fn [string-list]
      (transduce (map (partial format "%02x")) str
                 (let [auth ^Mac (.clone hmac-sha256)]
                   (loop [[s & r] string-list]
                     (let [bytes (.getBytes ^String s "ascii")]
                       (if (seq r)
                         (do (.update auth bytes) (recur r))
                         (.doFinal auth bytes)))))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Sockets

(defrecord SocketSystem [config]
  ILifecycle
  (init [{{:keys [transport ip shell-port iopub-port]} :config
          :as                                          this}]
    (let [ctx          (ZMQ/createContext)
          shell-socket (ZMQ/socket ctx ZMQ/ZMQ_ROUTER)
          iopub-socket (ZMQ/socket ctx ZMQ/ZMQ_PUB)
          addr         (partial str transport "://" ip ":")]
      (ZMQ/bind shell-socket (addr shell-port))
      (ZMQ/setSocketOption shell-socket ZMQ/ZMQ_RCVTIMEO (int 250))
      (ZMQ/bind iopub-socket (addr iopub-port))
      (assoc this
             :ctx ctx
             :shell-socket shell-socket
             :iopub-socket iopub-socket)))
  (close [{:keys [ctx] :as this}]
    (doseq [socket (vals (dissoc this :ctx :config))]
      (ZMQ/close socket))
    (ZMQ/term ctx)
    (log/info "All shell sockets closed.")))

(defn create-sockets [config]
  (init (->SocketSystem config)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Messaging

(def DELIM "<IDS|MSG>")

;;; Send

(defn map->blobs [{:keys [identities] :as message} signer]
  (let [data-blobs (map (comp util/edn->json message)
                        [:header :parent-header :metadata :content])
        signature  (signer data-blobs)]
    (concat identities [DELIM signature] data-blobs)))

(defn send-message [^SocketBase socket message signer]
  (loop [[^String msg & r :as l] (map->blobs message signer)]
    (log/debug "Sending " msg)
    (if (seq r)
      (do (ZMQ/send socket msg (+ ZMQ/ZMQ_SNDMORE ZMQ/ZMQ_DONTWAIT))
          (recur r))
      (ZMQ/send socket msg 0))))

;;; Receive

(defn receive-more? [socket]
  (pos? (ZMQ/getSocketOption socket ZMQ/ZMQ_RCVMORE)))

(defn blobs->map [blobs]
  (let [decoded-blobs             (map (fn [^Msg msg] (String. (.data msg))) blobs)
        [ids [delim sign & data]] (split-with (complement #{DELIM}) decoded-blobs)]
    (-> (zipmap [:header :parent-header :metadata :content]
                (map util/json->edn data))
        (assoc :signature sign :identities ids))))

(defn recv-message [socket]
  (loop [msg []]
    (when-let [blob (ZMQ/recv socket 0)]
      (let [msg (conj msg blob)]
        (if (receive-more? socket)
          (recur msg)
          (blobs->map msg))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; REPL

(defprotocol IREPL
  (eval-code [this code])
  (eval-code-to-str [this code]))

(defrecord REPL []
  ILifecycle
  (init [this]
    (let [{:keys [port] :as server} (nrepl.server/start-server)
          conn                      (nrepl/connect :port port)
          client                    (nrepl/client-session (nrepl/client conn 1000))]
      (assoc this
             :server server
             :port port
             :conn conn
             :client client)))
  (close [{:keys [server conn]}]
    (.close ^FnTransport conn)
    (nrepl.server/stop-server server))
  IREPL
  (eval-code [{:keys [client]} code]
    (let [responses (nrepl/message client {:op :eval :code code})]
      (if-let [err (some :err responses)]
        {:status :error :value err}
        {:status :ok :value (transduce
                             (comp (filter (comp #{:value :out} first))
                                   (map (fn [[k v]]
                                          (case k
                                            :value (->> (read-string v)
                                                        pprint/pprint
                                                        with-out-str)
                                            :out   v))))
                             conj
                             []
                             (apply concat responses))})))
  (eval-code-to-str [this code]
    (let [{:keys [status value]} (eval-code this code)]
      (case status
        :error value
        :ok    (apply str value)))))

(defn start-repl []
  (init (->REPL)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Handler

(defn response-map [config {:keys [shell-socket iopub-socket]} repl shutdown-signal]
  (let [signer          (signer-fn (:key config))
        execution-count (volatile! 0)]
    {"kernel_info_request"
     (fn [{{:keys [username session] :as header} :header
           :keys                                 [identities]}]
       (send-message shell-socket
                     {:identities    identities
                      :header        {:msg-id   (str (java.util.UUID/randomUUID))
                                      :date     (util/now)
                                      :version  "5.0"
                                      :username username
                                      :session  session
                                      :msg-type "kernel_info_reply"}
                      :content       {:protocol-version       "5.0"
                                      :implementation         "IClojure"
                                      :implementation-version "0.1.0"
                                      :language-info
                                      {:name           "clojure"
                                       :version        "1.8.0"
                                       :file-extension ".clj"}}
                      :parent-header header
                      :metadata      {}}
                     signer))

     "execute_request"
     (fn [{{:keys [username session] :as header} :header
           {:keys [code]}                        :content
           :keys                                 [identities]}]
       (log/debug "Executing" code)
       (vswap! execution-count inc)
       (send-message iopub-socket
                     {:identities    identities
                      :header        {:msg-id   (str (java.util.UUID/randomUUID))
                                      :date     (util/now)
                                      :version  "5.0"
                                      :username username
                                      :session  session
                                      :msg-type "status"}
                      :content       {:execution-state "busy"}
                      :parent-header header
                      :metadata      {}}
                     signer)
       (send-message iopub-socket
                     {:identities    identities
                      :header        {:msg-id   (str (java.util.UUID/randomUUID))
                                      :date     (util/now)
                                      :version  "5.0"
                                      :username username
                                      :session  session
                                      :msg-type "execute_input"}
                      :content       {:execution-count @execution-count
                                      :code            code}
                      :parent-header header
                      :metadata      {}}
                     signer)
       (send-message iopub-socket
                     {:identities    identities
                      :header        {:msg-id   (str (java.util.UUID/randomUUID))
                                      :date     (util/now)
                                      :version  "5.0"
                                      :username username
                                      :session  session
                                      :msg-type "execute_result"}
                      :content       {:execution-count @execution-count
                                      :data
                                      {"text/plain" (eval-code-to-str repl code)}}
                      :parent-header header
                      :metadata      {}}
                     signer)
       (send-message iopub-socket
                     {:identities    identities
                      :header        {:msg-id   (str (java.util.UUID/randomUUID))
                                      :date     (util/now)
                                      :version  "5.0"
                                      :username username
                                      :session  session
                                      :msg-type "status"}
                      :content       {:execution-state "idle"}
                      :parent-header header
                      :metadata      {}}
                     signer)
       (send-message shell-socket
                     {:identities    identities
                      :header        {:msg-id   (str (java.util.UUID/randomUUID))
                                      :date     (util/now)
                                      :version  "5.0"
                                      :username username
                                      :session  session
                                      :msg-type "execute_reply"}
                      :content       {:status          "ok"
                                      :execution-count @execution-count}
                      :parent-header header
                      :metadata      {}}
                     signer))

     "shutdown_request"
     (fn [{{:keys [username session] :as header} :header
           :keys                                 [identities]}]
       (deliver shutdown-signal true)
       (send-message shell-socket
                     {:identities    identities
                      :header        {:msg-id   (str (java.util.UUID/randomUUID))
                                      :date     (util/now)
                                      :version  "5.0"
                                      :username username
                                      :session  session
                                      :msg-type "shutdown_reply"}
                      :content       {:restart false}
                      :parent-header header
                      :metadata      {}}
                     signer))}))

(defn handler-fn [config socket-map repl shutdown-signal]
  (let [msg-type->response (response-map config socket-map repl shutdown-signal)]
    (fn [{{:keys [msg-type]} :header :as msg}]
      (log/info "Handling" msg)
      (if-let [handler (msg-type->response msg-type)]
        (handler msg)
        (log/info "No handler for" msg-type)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Worker

(defn start [config shutdown-signal]
  (future
    (log/info "Starting shell...")
    (try
      (with-open [^SocketSystem socket-system (create-sockets config)
                  ^REPL repl                  (start-repl)]
        (let [handler      (handler-fn config
                                       (dissoc socket-system :config :ctx)
                                       repl
                                       shutdown-signal)
              shell-socket (:shell-socket socket-system)]
          (log/debug "Entering loop...")
          (while (not (realized? shutdown-signal))
            (when-let [msg (recv-message shell-socket)]
              (handler msg)))))
      (catch Exception e (log/debug e))
      (finally
        (deliver shutdown-signal true)))))
