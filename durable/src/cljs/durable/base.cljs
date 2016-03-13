(ns durable.base
  (:require-macros
    [cljs.core :refer [es6-iterable]]))

(defprotocol XIterator
  (^Long xiindex [this])
  (xibumpIndex [this index])
  (xicount [this index])
  (xifetch [this index]))

(defn- -indexOf
  ([coll x]
   (-indexOf coll x 0))
  ([coll x start]
   (let [len (count coll)]
     (if (>= start len)
       -1
       (loop [idx (cond
                    (pos? start) start
                    (neg? start) (max 0 (+ start len))
                    :else start)]
         (if (< idx len)
           (if (= (nth coll idx) x)
             idx
             (recur (inc idx)))
           -1))))))

(defn- -lastIndexOf
  ([coll x]
   (-lastIndexOf coll x (count coll)))
  ([coll x start]
   (let [len (count coll)]
     (if (zero? len)
       -1
       (loop [idx (cond
                    (pos? start) (min (dec len) start)
                    (neg? start) (+ len start)
                    :else start)]
         (if (>= idx 0)
           (if (= (nth coll idx) x)
             idx
             (recur (dec idx)))
           -1))))))

(deftype CountedSequence [iter i styp meta]
  Object
  (toString [coll]
    (pr-str* coll))
  (equiv [this other]
    (-equiv this other))
  (indexOf [coll x]
    (-indexOf coll x 0))
  (indexOf [coll x start]
    (-indexOf coll x start))
  (lastIndexOf [coll x]
    (-lastIndexOf coll x (count coll)))
  (lastIndexOf [coll x start]
    (-lastIndexOf coll x start))

  ICloneable
  (-clone [_] (CountedSequence. iter i styp meta))

  ISeqable
  (-seq [this]
    (when (<= 0 (xicount iter i))
      this))

  IMeta
  (-meta [coll] meta)
  IWithMeta
  (-with-meta [coll new-meta]
    (CountedSequence. iter i styp new-meta))

  ASeq
  ISeq
  (-first [_] (styp (xifetch iter i)))
  (-rest [_]
    (if (< 1 (xicount iter i))
      (CountedSequence. iter (xibumpIndex iter i) styp nil)
      (list)))

  INext
  (-next [_]
    (if (< 1 (xicount iter i))
      (CountedSequence. iter (xibumpIndex iter i) styp nil)
      nil))

  ICounted
  (-count [_]
    (max 0 (xicount iter i)))

  IIndexed
  (-nth [coll n]
    (let [i (+ n i)]
      (when (>= 0 (xicount iter i))
        (styp (xifetch iter i)))))
  (-nth [coll n not-found]
    (let [i (+ n i)]
      (if (>= 0 (xicount iter i))
        (styp (xifetch iter i))
        not-found)))

  ISequential
  IEquiv
  (-equiv [coll other] (equiv-sequential coll other))

  IIterable
  (-iterator [coll]
    iter)

  ICollection
  (-conj [coll o] (cons o coll))

  IEmptyableCollection
  (-empty [coll] (.-EMPTY List))

  IHash
  (-hash [coll] (hash-ordered-coll coll))

  IReversible
  (-rseq [coll]
    (let [c (-count coll)]
      (if (pos? c)
        (RSeq. coll (dec c) nil))))

  IPrintWithWriter
  (-pr-writer [coll writer opts] (pr-sequential-writer writer pr-writer "(" " " ")" opts coll))
  )

(es6-iterable CountedSequence)

(defn create [iter initialIndex styp]
  (if (< 0 (xicount iter initialIndex))
    (CountedSequence. iter initialIndex styp nil)
    nil))

(defprotocol INoded
  (-getState [this]))

(defprotocol INode
  (-newNode [this t2 level left right cnt opts])
  (-getT2 [this opts])
  (-getLevel [this opts])
  (-getLeft [this opts])
  (-getRight [this opts])
  (-getCnt [this opts])
  (-getNada [this]))

(defn newMapEntry [k v] [k v])
