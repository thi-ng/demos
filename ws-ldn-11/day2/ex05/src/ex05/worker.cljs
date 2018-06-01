(ns worker
  (:require
   [thi.ng.strf.core :as f]
   [cljs.reader :refer [read-string]]))

;; base.js contains all of cljs.core etc.
(.importScripts js/self "base.js")

;; worker's app state
(def state (atom {}))

(defn start-ping
  "Command handler to start (and keep) pinging main app with given
  interval. Pinging stops when :active? key is false."
  [delay]
  (js/setTimeout
   (fn []
     (let [buf (.-buffer (js/Uint8Array. 1e7))
           t   (f/timestamp)]
       (.postMessage
        js/self
        #js [t buf (str "Worker running (" (f/now) ")")]
        #js [buf]))
     (when (:active? @state)
       (start-ping delay)))
   delay)
  (swap! state assoc :active? true))

(defn stop-ping
  "Command handler to stop pinging main app."
  [] (swap! state assoc :active? false))

(defn dispatch-command
  "Worker's onmessage handler. Decodes message as EDN and dispatches
  to command handlers based on :command key in message."
  [msg]
  (let [msg (read-string (.-data msg))]
    (case (keyword (:command msg))
      :start (start-ping (:interval msg))
      :stop  (stop-ping)
      (.warn js/console (str "unknown worker command: " (:command msg))))))

(set! (.-onmessage js/self) dispatch-command)
