(ns durable.games
  (:require
    [durable.csq-test :as csq-test]
    [durable.nodes-test :as nodes-test]
;    [durable.AAVectorTest :as AAVectorTest]
    ))

#?(:clj
   (set! *warn-on-reflection* true))

(defn bingo []
  (csq-test/cstest)
  (nodes-test/ntest)
;  (AAVectorTest/vtest)
  )
