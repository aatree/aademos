(ns checksum.fun-test
  (:require [clojure.test :refer :all]
            [checksum.games :as games]))

(set! *warn-on-reflection* true)

(println 123)
(games/bingo)
