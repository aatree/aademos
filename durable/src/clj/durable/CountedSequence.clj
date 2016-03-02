(ns durable.CountedSequence
  (:gen-class
    :main false
    :extends clojure.lang.ASeq
    :implements [clojure.lang.Counted]
    :constructors {[java.util.Iterator Long clojure.lang.IFn]
                   []
                   [clojure.lang.IPersistentMap Object]
                   [clojure.lang.IPersistentMap]}
    :init init
    :state state
    :methods [^:static [create [java.util.Iterator Long clojure.lang.IFn] Object]])
  (:import (java.util Iterator)
           (clojure.lang Counted)
           (durable CountedSequence)))

(set! *warn-on-reflection* true)

(defprotocol XIterator
  (^Long xiindex [this])
  (xibumpIndex [this index])
  (xicount [this index])
  (xifetch [this index]))

(defn -create [iter initialIndex styp]
  (if (< 0 (xicount iter initialIndex))
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
    (xifetch (iter s) (:ndx s))))

(defn -next [^CountedSequence this]
  (let [s (.-state this)
        ^XIterator it (iter s)
        r (:rst s)]
    (when (= s @r)
      (-first this)
      (swap! r #(if (= s %) (-create it (xibumpIndex it (:ndx s)) (:styp s)))))
    @(:rst s)))

(defn -count [^CountedSequence this]
  (let [s (.-state this)]
    (xicount (iter s) (:ndx s))))
