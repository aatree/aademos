(ns durable.games
  (:require
    [durable.CountedSequenceTest :as CountedSequenceTest]
    [durable.nodes-test :as nodes-test]))

#?(:clj
   (set! *warn-on-reflection* true))

(defn bingo []
  (CountedSequenceTest/cstest)
  (nodes-test/ntest))
