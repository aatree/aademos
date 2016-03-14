(ns durable.CountedSequence
  #?(:clj(:gen-class
    :main false
    :extends clojure.lang.ASeq
    :implements [clojure.lang.Counted]
    :constructors {[java.util.Iterator Long clojure.lang.IFn]
                   []
                   [clojure.lang.IPersistentMap Object]
                   [clojure.lang.IPersistentMap]}
    :init init
    :state state
    :methods [^:static [create [java.util.Iterator Long clojure.lang.IFn] Object]]))
  (:require [durable.base :as base])
  #?(:clj(:import (java.util Iterator)
           (clojure.lang Counted)
           (durable CountedSequence))))
#?(:clj
   (do
     (set! *warn-on-reflection* true)

     (defn -create [iter initialIndex styp]
       (if (< 0 (base/xicount iter initialIndex))
         (new durable.CountedSequence iter initialIndex styp)
         nil))

     (defrecord seq-state [iter ndx styp rst])

     (defn iter [seq-state] (:iter seq-state))

     (defn -init
       ([^Iterator iter initialIndex styp]
        (let [^Counted citer iter
              s (->seq-state iter initialIndex styp (atom nil))]
          (reset! (:rst s) s)
          [[] s]))
       ([meta s]
        [[meta] s]))

     (defn -withMeta [^CountedSequence this meta] (new durable.CountedSequence meta (.-state this)))

     (defn -first [^CountedSequence this]
       (let [s (.-state this)]
         ((:styp s) (base/xifetch (iter s) (:ndx s)))))

     (defn -next [^CountedSequence this]
       (let [s (.-state this)
             it (iter s)
             r (:rst s)]
         (when (= s @r)
           (-first this)
           (swap! r #(if (= s %) (-create it (base/xibumpIndex it (:ndx s)) (:styp s)))))
         @(:rst s)))

     (defn -count [^CountedSequence this]
       (let [s (.-state this)]
         (base/xicount (iter s) (:ndx s)))))
   :cljs
   (do
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
         (when (<= 0 (base/xicount iter i))
           this))

       IMeta
       (-meta [coll] meta)
       IWithMeta
       (-with-meta [coll new-meta]
         (CountedSequence. iter i styp new-meta))

       ASeq
       ISeq
       (-first [_] (styp (base/xifetch iter i)))
       (-rest [_]
         (if (< 1 (base/xicount iter i))
           (CountedSequence. iter (base/xibumpIndex iter i) styp nil)
           (list)))

       INext
       (-next [_]
         (if (< 1 (base/xicount iter i))
           (CountedSequence. iter (base/xibumpIndex iter i) styp nil)
           nil))

       ICounted
       (-count [_]
         (max 0 (base/xicount iter i)))

       IIndexed
       (-nth [coll n]
         (let [i (+ n i)]
           (when (>= 0 (base/xicount iter i))
             (styp (base/xifetch iter i)))))
       (-nth [coll n not-found]
         (let [i (+ n i)]
           (if (>= 0 (base/xicount iter i))
             (styp (base/xifetch iter i))
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
       (if (< 0 (base/xicount iter initialIndex))
         (CountedSequence. iter initialIndex styp nil)
         nil))
   ))
