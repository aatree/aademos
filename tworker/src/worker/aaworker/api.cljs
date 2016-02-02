(ns aaworker.api)

(def worker-fn-map (atom {}))

(defn register-fn [fn-name f]
  (swap! worker-fn-map assoc (keyword fn-name) f))
