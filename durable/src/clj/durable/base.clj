(ns durable.base)

(set! *warn-on-reflection* true)

(defprotocol XIterator
  (^Long xiindex [this])
  (xibumpIndex [this index])
  (xicount [this index])
  (xifetch [this index]))
