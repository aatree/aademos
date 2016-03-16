(ns durable.CountedSequenceTest
  (:require
    [durable.base :as base]
    [durable.csq :as csq])
  #?(:clj (:import (clojure.lang Counted)
                   (java.util Iterator))))

  #?(:clj
     (set! *warn-on-reflection* true))

(deftype vector-iterator
  [v ^{:volatile-mutable true} ndx]

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

(defn create-counted-sequence [iter initialIndex styp]
  (csq/create iter initialIndex styp))

(defn new-counted-seq
  ([v]
   (let [it (new-vector-iterator v)]
     (create-counted-sequence it (base/xiindex it) identity)))
  ([v i]
   (let [it (new-vector-iterator v i)]
     (create-counted-sequence it (base/xiindex it) identity))))

  (defn cstest []
        (def s23 (new-counted-seq [1 2 3] 1))
        (println (count s23))
        (println s23)
        (println (first s23))

        (def s3 (next s23))
        (println (count s3))
        (println s3)
        (println (first s3))

        (println (next s3)))
