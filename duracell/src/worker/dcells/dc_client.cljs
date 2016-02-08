(ns dcells.dc-client
  (:require-macros
    [aaworker.worker-macros :refer [deflpc!]])
  (:require
    [aaworker.api :as api]))

(defn start []
  (api/process-requests))