(ns checksum.games
  (:require [checksum.bytes :as bytes]))

#?(:clj
   (set! *warn-on-reflection* true))

(defn bingo []
  (println "abc")

  (def ba1 (bytes/make-bytes 3))
  (bytes/set-byte! ba1 1 -3)
  (println (bytes/get-byte ba1 1))
  (println (bytes/vec-bytes ba1))

  (def ba2 (bytes/make-bytes 3))
  (bytes/set-byte! ba2 1 -3)
  (println (bytes/bytes-equal ba1 ba2))

  (def ba3 (bytes/make-bytes 3))
  (bytes/set-byte! ba3 2 -3)
  (println (bytes/bytes-equal ba1 ba3))

  (println (bit-xor 5 6)))
