(ns durable.seq-test
  (:require [durable.CountedSequence :refer :all])
  (:import (clojure.lang Counted)
           (java.util Iterator)
           (durable CountedSequence)))

(set! *warn-on-reflection* true)

(deftype vector-iterator
  [v
   ^{:volatile-mutable true Long true} ndx]

  XIterator
  (xicount [this index]
    (- (count v) index))
  (xiindex [this]
    ndx)
  (xibumpIndex [this index]
    (+ 1 index))
  (xifetch [this index]
    (v index))

  Counted
  (count [this]
    (xicount this ndx))

  Iterator
  (hasNext [this]
    (< ndx (count v)))
  (next [this]
    (let [i ndx]
      (set! ndx (xibumpIndex this i))
      (xifetch this i))))

(defn ^vector-iterator new-vector-iterator
  ([v]
   (->vector-iterator v 0))
  ([v i]
   (->vector-iterator v i)))

(defn ^CountedSequence new-counted-seq
  ([v]
   (let [it (new-vector-iterator v)]
     (CountedSequence/create it (xiindex it) identity)))
  ([v i]
   (let [it (new-vector-iterator v i)]
     (CountedSequence/create it (xiindex it) identity))))

(def s23 (new-counted-seq [1 2 3] 1))
(println (count s23))
(println s23)
(println (first s23))

(def s3 (next s23))
(println (count s3))
(println s3)
(println (first s3))

(println (next s3))
