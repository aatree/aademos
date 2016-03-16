(ns durable.AAVector
  #?(:clj
     (:gen-class
       :main false
       :extends clojure.lang.APersistentVector
       :implements [clojure.lang.IObj durable.base.INoded]
       :constructors {[java.lang.Object clojure.lang.IPersistentMap]
                      []
                      [java.lang.Object clojure.lang.IPersistentMap clojure.lang.IPersistentMap]
                      []}
       :init init
       :state state))
  (:require [durable.base :as base])
  #?(:clj
     (:import (durable AAVector)
              (durable.base INode)
              (clojure.lang IPersistentMap))))

#?(:clj
   (do
     (set! *warn-on-reflection* true)

     (defn -getState [^AAVector this]
       (.-state this))

     (defn -init
       ([node opts]
        [[] (base/->noded-state node opts nil)])
       ([node opts meta]
        [[] (base/->noded-state node opts meta)]))

     (defn -meta [^AAVector this] (base/get-meta this))

     (defn -withMeta [^AAVector this meta] (new AAVector (base/get-inode this) (base/get-opts this) meta))

     (defn -count [this]
       (base/-getCnt (base/get-inode this) (base/get-opts this)))

     (defn -nth
       ([^AAVector this i]
        (base/-nth-t2 (base/get-inode this) i (base/get-opts this)))
       ([this i notFound]
        (if (and (>= i 0) (< i (-count this)))
          (-nth this i)
          notFound)))

     (defn -cons [^AAVector this val]
       (let [n0 (base/get-inode this)
             n1 (base/vector-add n0 (base/transcriber val (base/get-opts this)) (-count this) (base/get-opts this))]
         (new AAVector n1 (base/get-opts this) (base/get-meta this))))

     (defn addNode [^AAVector this i val]
       (let [c (-count this)]
         (cond
           (= i c)
           (-cons this (base/transcriber val (base/get-opts this)))
           (and (>= i 0) (< i c))
           (let [n0 (base/get-inode this)
                 n1 (base/vector-add n0 (base/transcriber val (base/get-opts this)) i (base/get-opts this))]
             (new AAVector n1 (base/get-opts this) (base/get-meta this)))
           :else
           (throw (IndexOutOfBoundsException.)))))

     (defn -vector-set [n v i opts]
       (if (base/empty-node? n)
         (base/-newNode n v 1 nil nil 1 opts)
         (let [l (base/left-node n opts)
               p (base/-getCnt l opts)]
           (base/-split
             (base/-skew
               (cond
                 (< i p)
                 (base/-revise n [:left (-vector-set l v i opts)] opts)
                 (> i p)
                 (base/-revise n [:right (-vector-set (base/right-node n opts) v (- i p 1) opts)] opts)
                 :else
                 (base/-revise n [:t2 v] opts))
               opts)
             opts))))

     (defn -assocN [^AAVector this i val]
       (let [c (-count this)]
         (cond
           (= i c)
           (-cons this (base/transcriber val (base/get-opts this)))
           (and (>= i 0) (< i c))
           (let [n0 (base/get-inode this)
                 n1 (-vector-set n0 (base/transcriber val (base/get-opts this)) i (base/get-opts this))]
             (new AAVector n1 (base/get-opts this) (base/get-meta this)))
           :else
           (throw (IndexOutOfBoundsException.)))))

     (defn -empty [^AAVector this]
       (new AAVector
            (base/empty-node (base/get-inode this) (base/get-opts this))
            (base/get-opts this)
            (base/get-meta this)))

     (defn -iterator [^AAVector this]
       (base/-new-counted-iterator (base/get-inode this) (base/get-opts this)))

     (defn -seq
       [^AAVector this]
       (base/-new-counted-seq (base/get-inode this) (base/get-opts this)))

     (defn -pop [^AAVector this]
       (if (empty? this)
         this
         (let [n0 (base/get-inode this)
               n1 (base/deln n0 (- (-count this) 1) (base/get-opts this))]
           (new AAVector n1 (base/get-opts this) (base/get-meta this)))))

     (defn dropNode [^AAVector this i]
       (if (or (< i 0) (>= i (-count this)))
         this
         (new AAVector
              (base/deln (base/get-inode this) i (base/get-opts this))
              (base/get-opts this)
              (base/get-meta this)))))
   :cljs
   (do
     (defprotocol FlexVector
       (-dropNode [this i])
       (-addNode [this i v]))
     ))
