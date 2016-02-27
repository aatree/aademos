(ns checksum.bytes)

(set! *warn-on-reflection* true)

(defn make-bytes [s]
  (byte-array s))

(defn set-byte! [^"[B" a i v]
  (aset-byte a i v))

(defn get-byte [^"[B" a i]
  (aget a i))

(defn bytes-equal [^"[B" a1 ^"[B" a2]
  (java.util.Arrays/equals a1 a2))

(defn vec-bytes [a]
  (vec a))
