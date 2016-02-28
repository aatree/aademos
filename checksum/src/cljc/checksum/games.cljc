(ns checksum.games
  (:require [aautil.bytes :as bytes]
            [aautil.cs256 :as cs256]))

#?(:clj
   (set! *warn-on-reflection* true))

(defn bingo []
  (def cs1 (cs256/make-cs256))
  (println (bytes/vec-bytes cs1))
  (cs256/digest-byte! cs1 -128)
  (println (bytes/vec-bytes cs1))
  (cs256/digest-byte! cs1 -128)
  (println (bytes/vec-bytes cs1))

  (def cs2 (cs256/make-cs256))
  (def ba (bytes/make-bytes 2))
  (cs256/digest! cs2 ba)
  (println (bytes/vec-bytes cs2))
  (println (bytes/bytes-equal cs1 cs2)))
