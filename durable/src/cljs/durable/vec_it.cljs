(ns durable.vec-it
  (:require [durable.CountedSequence :as cs]))

(deftype vector-iterator
  [v ^{:volatile-mutable true} ndx]

  Object
  (hasNext [this]
    (< ndx (count v)))
  (next [this]
    (let [i ndx]
      (set! ndx (cs/xibumpIndex this i))
      (cs/xifetch this i)))

  cs/XIterator
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
    (cs/xicount this ndx))
)

(defn ^vector-iterator new-vector-iterator
  ([v]
   (->vector-iterator v 0))
  ([v i]
   (->vector-iterator v i)))

(defn ^cs/CountedSequence new-counted-seq
  ([v]
   (let [it (new-vector-iterator v)]
     (cs/create it (cs/xiindex it) identity)))
  ([v i]
   (let [it (new-vector-iterator v i)]
     (cs/create it (cs/xiindex it) identity))))
