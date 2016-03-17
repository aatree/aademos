(ns durable.csq
  (:require
    [durable.base :as base])
  #?(:clj
     (:import
       (java.util Iterator)
       (clojure.lang Counted))))
#?(:clj
   (do
     (set! *warn-on-reflection* true)

     (defrecord seq-state [iter ndx styp rst])

     (declare create)

     (defn new-csq [meta state]
       (proxy [clojure.lang.ASeq clojure.lang.Counted] [meta]
         (withMeta [meta] (new-csq meta state ))

         (first []
           ((:styp state) (base/xifetch (:iter state) (:ndx state))))

         (next []
           (let [it (:iter state)
                 r (:rst state)]
             (when (= state @r)
               (first this)
               (swap! r #(if (= state %) (create it (base/xibumpIndex it (:ndx state)) (:styp state)))))
             @(:rst state)))

         (count []
           (base/xicount (:iter state) (:ndx state)))
         ))

     (defn new-counted-sequence [iter ndx styp]
       (let [state (->seq-state iter ndx styp (atom nil))]
         (reset! (:rst state) state)
         (new-csq nil state)))

     (defn create [iter initialIndex styp]
       (if (< 0 (base/xicount iter initialIndex))
         (new-counted-sequence iter initialIndex styp)
         nil))
     )
   :cljs
   (do
     (deftype counted-sequence [iter i styp meta]
       Object
       (toString [coll]
         (pr-str* coll))
       (equiv [this other]
         (-equiv this other))
       (indexOf [coll x]
         (base/-indexOf coll x 0))
       (indexOf [coll x start]
         (base/-indexOf coll x start))
       (lastIndexOf [coll x]
         (base/-lastIndexOf coll x (count coll)))
       (lastIndexOf [coll x start]
         (base/-lastIndexOf coll x start))

       ICloneable
       (-clone [_] (counted-sequence. iter i styp meta))

       ISeqable
       (-seq [this]
         (when (<= 0 (base/xicount iter i))
           this))

       IMeta
       (-meta [coll] meta)
       IWithMeta
       (-with-meta [coll new-meta]
         (counted-sequence. iter i styp new-meta))

       ASeq
       ISeq
       (-first [_] (styp (base/xifetch iter i)))
       (-rest [_]
         (if (< 1 (base/xicount iter i))
           (counted-sequence. iter (base/xibumpIndex iter i) styp nil)
           (list)))

       INext
       (-next [_]
         (if (< 1 (base/xicount iter i))
           (counted-sequence. iter (base/xibumpIndex iter i) styp nil)
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

     (es6-iterable counted-sequence)

     (defn create [iter initialIndex styp]
       (if (< 0 (base/xicount iter initialIndex))
         (counted-sequence. iter initialIndex styp nil)
         nil))
     ))
