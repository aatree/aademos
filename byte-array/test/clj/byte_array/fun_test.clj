(ns byte-array.fun-test
  (:require [clojure.test :refer :all]
            [byte-array.games :as games]))

(set! *warn-on-reflection* true)

(println 123)

(games/bingo)
