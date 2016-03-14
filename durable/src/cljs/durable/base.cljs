(ns durable.base
  (:require-macros
    [cljs.core :refer [es6-iterable]]))

(defprotocol XIterator
  (^Long xiindex [this])
  (xibumpIndex [this index])
  (xicount [this index])
  (xifetch [this index]))

(defprotocol INoded
  (-getState [this]))

(defprotocol INode
  (-newNode [this t2 level left right cnt opts])
  (-getT2 [this opts])
  (-getLevel [this opts])
  (-getLeft [this opts])
  (-getRight [this opts])
  (-getCnt [this opts])
  (-getNada [this])
  (-new-counted-iterator [this opts])
  (-new-counted-seq [this opts])
  )

(defn newMapEntry [k v] [k v])

(defn empty-node? [n]
  (or (nil? n) (identical? n (-getNada n))))

(defn empty-node [this opts]
  (if (empty-node? this)
    this
    (-getNada this)))

(defn left-node [this opts]
  (if (empty-node? (-getLeft this opts))
    (empty-node this opts)
    (-getLeft this opts)))

(defn right-node [this opts]
  (if (empty-node? (-getRight this opts))
    (empty-node this opts)
    (-getRight this opts)))
