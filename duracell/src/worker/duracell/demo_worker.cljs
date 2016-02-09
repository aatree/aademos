(ns duracell.demo-worker
  (:require-macros
    [aaworker.worker-macros :refer [deflpc! deflapc!]])
  (:require
    [aaworker.api :as api]
    [dcells.dc-worker :as dc]))

(set! cljs.core/*print-fn* #(.log js/console %))

(deflapc! load-txt []
          (dc/load-cell success failure "txt"))

(defn main []
  (dc/start "myDB1"))