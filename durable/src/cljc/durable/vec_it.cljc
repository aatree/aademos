(ns durable.vec-it
  (:require [durable.base :as base])
  #?(:clj (:require [durable.CountedSequence :refer :all]))
  #?(:clj (:import (clojure.lang Counted)
                   (java.util Iterator)
                   (durable CountedSequence))))

#?(:clj
   (set! *warn-on-reflection* true))

(deftype vector-iterator
  [v
   ^{:volatile-mutable true} ndx]

  #?@(:cljs (Object
              (hasNext [this]
                       (< ndx (count v)))
              (next [this]
                    (let [i ndx]
                      (set! ndx (base/xibumpIndex this i))
                      (base/xifetch this i))))
      :clj (Iterator
             (hasNext [this]
               (< ndx (count v)))
             (next [this]
               (let [i ndx]
                 (set! ndx (base/xibumpIndex this i))
                 (base/xifetch this i)))))

  base/XIterator
  (xicount [this index]
    (- (count v) index))
  (xiindex [this]
    ndx)
  (xibumpIndex [this index]
    (+ 1 index))
  (xifetch [this index]
    (v index))

  #?@(:cljs(ICounted
             (-count [this]
                     (base/xicount this ndx)))
      :clj(Counted
            (count [this]
              (base/xicount this ndx)))))

(defn ^vector-iterator new-vector-iterator
  ([v]
   (->vector-iterator v 0))
  ([v i]
   (->vector-iterator v i)))

#?(:clj (defn ^CountedSequence new-counted-seq
  ([v]
   (let [it (new-vector-iterator v)]
     (CountedSequence/create it (base/xiindex it) identity)))
  ([v i]
   (let [it (new-vector-iterator v i)]
     (CountedSequence/create it (base/xiindex it) identity)))))

#?(:cljs (defn ^base/CountedSequence new-counted-seq
  ([v]
   (let [it (new-vector-iterator v)]
     (base/create it (base/xiindex it) identity)))
  ([v i]
   (let [it (new-vector-iterator v i)]
     (base/create it (base/xiindex it) identity)))))
