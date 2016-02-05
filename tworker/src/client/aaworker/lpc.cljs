(ns aaworker.lpc)

(def worker-map (atom {}))

(defn new-worker [file-name]
  (let [w (js/Worker. file-name)]
    (swap! worker-map assoc file-name
           [w {}])
;    (set! (.-onmessage @w) #(println "from worker" (.-data %)))
    ))

(defn register-responder [file-name fn-name rsp-vec]
  (swap! worker-map assoc-in [file-name 1 (keyword fn-name)] rsp-vec))

(defn mklocal [fn-name file-name state error loading]
  (register-responder file-name fn-name [state error loading])
  (fn [& args]
    (let [msg (prn-str (keyword fn-name))
          w (get-in @worker-map [file-name 0])]
      .post w msg)))
