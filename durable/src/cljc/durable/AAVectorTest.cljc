(ns durable.AAVectorTest
  (:require [durable.core :as core]))

#?(:clj
   (set! *warn-on-reflection* true))

(defn vtest[]
  #?(:clj
     (do
       (def opts (core/basic-opts))

       (def bv1 (conj (core/new-vector opts) 1 2 3))
       (println bv1); -> [1 2 3]
       )))
