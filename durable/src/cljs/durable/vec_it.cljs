(ns durable.vec-it
  (:require [durable.base :as base]))

(deftype vector-iterator
  [v ^{:volatile-mutable true} ndx]

  Object
  (hasNext [this]
    (< ndx (count v)))
  (next [this]
    (let [i ndx]
      (set! ndx (base/xibumpIndex this i))
      (base/xifetch this i)))

  base/XIterator
  (xicount [this index]
    (- (count v) index))
  (xiindex [this]
    ndx)
  (xibumpIndex [this index]
    (+ 1 index))
  (xifetch [this index]
    (v index))

  ICounted
  (-count [this]
    (base/xicount this ndx))
)

(defn ^vector-iterator new-vector-iterator
  ([v]
   (->vector-iterator v 0))
  ([v i]
   (->vector-iterator v i)))

(defn ^base/CountedSequence new-counted-seq
  ([v]
   (let [it (new-vector-iterator v)]
     (base/create it (base/xiindex it) identity)))
  ([v i]
   (let [it (new-vector-iterator v i)]
     (base/create it (base/xiindex it) identity))))
