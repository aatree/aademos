(ns durable.games
  (:require [durable.vec-it :as vec-it]
            [durable.base :as base]
            [durable.aabuffer :as buffer]))

#?(:clj
   (set! *warn-on-reflection* true))

(defn bingo []
  (def s23 (vec-it/new-counted-seq [1 2 3] 1))
  (println (count s23))
  (println s23)
  (println (first s23))

  (def s3 (next s23))
  (println (count s3))
  (println s3)
  (println (first s3))

  (println (next s3))

  (def me (base/newMapEntry 1 2))
  (println (key me) (val me))

  (def b (buffer/newBuffer 8))
  (println (buffer/-capacity b))
  (println (buffer/-position b))
  (println (buffer/-limit b))
  (buffer/-position! b 4)
  (buffer/-limit! b 6)
  (println (buffer/-position b))
  (println (buffer/-limit b))
  (buffer/-clear! b)
  (println (buffer/-position b))
  (println (buffer/-limit b))
  (buffer/-position! b 4)
  (buffer/-flip! b)
  (println (buffer/-position b))
  (println (buffer/-limit b))
  )
