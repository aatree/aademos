(ns tworker.demo-worker
;  (:require-macros
;    [aaworker.worker_macros :refer [deflpc]])
;  (:require
;    [aaworker.api])
  )

;(set! cljs.core/*print-fn* #(.log js/console %))

(defn process-message [event]
  (let [data (.-data event)]
;    (println data)
    (.postMessage js/self "Ho!")))

(defn main []
;  (println "worker start")
  (set! (.-onmessage js/self) #(println (.-data %))))