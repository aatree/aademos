(ns durable.base
  (:import (clojure.lang MapEntry)))

(set! *warn-on-reflection* true)

(defprotocol XIterator
  (^Long xiindex [this])
  (xibumpIndex [this index])
  (xicount [this index])
  (xifetch [this index]))

(defprotocol FlexVector
  (-dropNode [this i])
  (-addNode [this i v]))

(defn newMapEntry [k v] (MapEntry. k v))
