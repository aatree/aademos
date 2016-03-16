(ns durable.games
  (:require
    [durable.CountedSequenceTest :as CountedSequenceTest]
;    [durable.nodes-test :as nodes-test]
;    [durable.AAVectorTest :as AAVectorTest]
    ))

#?(:clj
   (set! *warn-on-reflection* true))

(defn bingo []
  (CountedSequenceTest/cstest)
;  (nodes-test/ntest)
;  (AAVectorTest/vtest)
  )
