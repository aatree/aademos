(ns aaworker.api)

(def worker-fn-map (atom {}))

(defn register-fn [fn-name f]
  (swap! worker-fn-map assoc (keyword fn-name) f))

(defn process-message [event]
  (let [data (.-data event)]
    (println data)
    (.postMessage js/self "Ho!")))

(defn process-requests []
  (set! (.-onmessage js/self) process-message))