(ns aaworker.lpc
  (:require [cljs.reader :refer [read-string]]))

(def worker-map (atom {}))

(defn new-worker [file-name]
  (let [w (js/Worker. file-name)]
    (swap! worker-map assoc file-name
           [w {}])
    (set! (.-onmessage w) #(println "from worker" (.-data %)))
    ))

(defn register-responder [file-name fn-name rsp-vec]
  (swap! worker-map assoc-in [file-name 1 (keyword fn-name)] rsp-vec))

(defn mklocal [fn-name file-name state error loading]
  (register-responder file-name fn-name [state error loading])
  (fn [& args]
    (let [msg [(keyword fn-name)]
          msg (if args
                (conj msg args)
                msg)
          msg (prn-str msg)
          w (get-in @worker-map [file-name 0])]
      (.postMessage w msg))))
