(ns durable.aavec
  (:require
    [durable.base :as base]))

#?(:clj (set! *warn-on-reflection* true))

(defprotocol FlexVector
  (dropNode [this i])
  (addNode [this i v]))

#?(:clj
   (do
     (defn vector-set [n v i opts]
       (if (base/empty-node? n)
         (base/-newNode n v 1 nil nil 1 opts)
         (let [l (base/left-node n opts)
               p (base/-getCnt l opts)]
           (base/-split
             (base/-skew
               (cond
                 (< i p)
                 (base/-revise n [:left (vector-set l v i opts)] opts)
                 (> i p)
                 (base/-revise n [:right (vector-set (base/right-node n opts) v (- i p 1) opts)] opts)
                 :else
                 (base/-revise n [:t2 v] opts))
               opts)
             opts))))

     (defn new-aavec [-root -opts -meta]
       (proxy [clojure.lang.APersistentVector clojure.lang.IObj durable.base.INoded durable.aavec.FlexVector] []

         (get-inode [] -root)

         (get-opts [] -opts)

         (get-meta [] -meta)

         (meta [] -meta)

         (withMeta [meta] (new-aavec -root -opts meta))

         (count []
           (base/-getCnt -root -opts))

         (nth
           ([i]
            (base/-nth-t2 -root i -opts))
           ([i notFound]
            (if (and (>= i 0) (< i (count this)))
              (nth this i)
              notFound)))

         (cons [val]
           (let [n0 -root
                 n1 (base/vector-add n0 (base/transcriber val -opts) (count this) -opts)]
             (new-aavec n1 -opts -meta)))

         (assocN [i val]
           (let [c (count this)]
             (cond
               (= i c)
               (cons this (base/transcriber val -opts))
               (and (>= i 0) (< i c))
               (let [n0 -root
                     n1 (vector-set n0 (base/transcriber val -opts) i -opts)]
                 (new-aavec n1 -opts -meta))
               :else
               (throw (IndexOutOfBoundsException.)))))

         (empty []
           (new-aavec (base/empty-node -root -opts) -opts -meta))

         (iterator []
           (base/-new-counted-iterator -root -opts))

         (seq []
           (base/-new-counted-seq -root -opts))

         (pop []
           (if (empty? this)
             this
             (let [n0 -root
                   n1 (base/deln n0 (- (count this) 1) -opts)]
               (new-aavec n1 -opts -meta))))

         (dropNode [i]
           (if (or (< i 0) (>= i (count this)))
             this
             (new-aavec (base/deln -root i -opts) -opts -meta)))

         (addNode [i val]
           (let [c (count this)]
             (cond
               (= i c)
               (cons this (base/transcriber val -opts))
               (and (>= i 0) (< i c))
               (let [n0 -root
                     n1 (base/vector-add n0 (base/transcriber val -opts) i -opts)]
                 (new-aavec n1 -opts -meta))
               :else
               (throw (IndexOutOfBoundsException.)))))
         ))

     (defn create [root opts]
       (new-aavec root opts nil))
     )
   :cljs
   (do
     ))
