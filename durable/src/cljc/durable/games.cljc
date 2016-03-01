(ns durable.games
  (:require [durable.vec-it :as vec-it]))

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
  )
