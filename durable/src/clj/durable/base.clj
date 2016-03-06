(ns durable.base)

(set! *warn-on-reflection* true)

(defprotocol XIterator
  (^Long xiindex [this])
  (xibumpIndex [this index])
  (xicount [this index])
  (xifetch [this index]))

(defprotocol INoded
  (-getState [this]))

(defprotocol AAContext
  (-classAtom [this])
  (-getDefaultFactory [this])
  (-setDefaultFactory [this factory])
  (-refineInstance [this inst]))

(defprotocol INode
  (-newNode [this t2 ^Long level left right ^Long cnt opts])
  (-getT2 [this opts])
  (^Long -getLevel [this opts])
  (-getLeft [this opts])
  (-getRight [this opts])
  (^Long -getCnt [this opts])
  (-getNada [this]))

(defprotocol FlexVector
  (-dropNode [this i])
  (-addNode [this i v]))
