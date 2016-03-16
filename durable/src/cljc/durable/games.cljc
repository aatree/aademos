(ns durable.games
  (:require
    [durable.csq-test :as csq-test]
    [durable.nodes-test :as nodes-test]
    [durable.aavec-test :as aavec-test]
    ))

#?(:clj
   (set! *warn-on-reflection* true))

(defn bingo []
  (csq-test/cstest)
  (nodes-test/ntest)
  (aavec-test/vtest)
  )
