(ns aaworker.lpc)

(def worker-map (atom {}))

(defn new-worker [file-name]
  (swap! worker-map assoc file-name
         [(js/Worker. file-name) {}]))

(defn register-responder [file-name fn-name rsp-vec]
  (swap! worker-map assoc-in [file-name (keyword fn-name)] rsp-vec))

(defn mklocal [fn-name file-name state error loading]
  (aaworker.lpc/register-responder file-name fn-name [state error loading]))
