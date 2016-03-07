(ns durable.base)

(set! *warn-on-reflection* true)

(defprotocol XIterator
  (^Long xiindex [this])
  (xibumpIndex [this index])
  (xicount [this index])
  (xifetch [this index]))

(defprotocol INoded
  (-getState [this]))

(defprotocol INode
  (-newNode [this t2 ^Long level left right ^Long cnt opts])
  (-getT2 [this opts])
  (^Long -getLevel [this opts])
  (-getLeft [this opts])
  (-getRight [this opts])
  (^Long -getCnt [this opts])
  (-getNada [this]))

(defprotocol WrapperNode
  (-svalAtom [this])
  (-blenAtom [this])
  (-bufferAtom [this])
  (-factory [this])
  (-nodeByteLength [this opts])
  (-nodeWrite [this buffer opts]))

(defprotocol AAContext
  (-classAtom [this])
  (-getDefaultFactory [this])
  (-setDefaultFactory [this factory])
  (-refineInstance [this inst]))

(defprotocol IFactory
  (-factoryId [this])
  (-instanceClass [this])
  (-qualified [this t2 opts])
  (-sval [this inode opts])
  (-valueLength [this node opts])
  (-deserialize [this node ^java.nio.ByteBuffer buffer opts])
  (-writeValue [this node ^java.nio.ByteBuffer buffer opts])
  (-valueNode [this node opts]))

(defprotocol FlexVector
  (-dropNode [this i])
  (-addNode [this i v]))
