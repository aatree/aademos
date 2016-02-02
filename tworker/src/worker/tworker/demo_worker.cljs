(ns tworker.demo-worker
  (:require-macros
    [aaworker.worker-macros :refer [deflpc]])
  (:require
    [aaworker.api :as api]))

(set! cljs.core/*print-fn* #(.log js/console %))

(def clicks (atom 0))

(deflpc click []
        (swap! clicks + 1)
        @clicks)

(println (keys @api/worker-fn-map))

(defn process-message [event]
  (let [data (.-data event)]
    (println data)
    (.postMessage js/self "Ho!")))

(defn main []
  (println "worker start")
  (set! (.-onmessage js/self) process-message))