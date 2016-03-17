(ns durable.aavec-test
  (:require [durable.core :as core]))

#?(:clj
   (set! *warn-on-reflection* true))

(defn vtest[]
  #?(:clj
     (do
       (println)
       (println "aavec-test")
       (println)

       (def opts (core/basic-opts))

       (def bv1 (conj (core/new-vector opts) 1 2 3))
       (println bv1); -> [1 2 3]

       (def bv2 (core/addn bv1 0 0))
       (println bv2); -> [0 1 2 3]

       (def bv3 (core/addn bv2 3 20))
       (println bv3); -> [0 1 2 20 3]

       (def bv4 (core/dropn bv3 1))
       (println bv4); -> [0 2 20 3]

       (def s1 (seq bv4))
       (println s1); -> (0 2 20 3)
       (println (count s1)); -> 4

       (def s2 (next s1))
       (println s2); -> (2 20 3)
       (println (count s2)); -> 3

       (println (rseq bv4)); -> (3 20 2 0)
       )))
