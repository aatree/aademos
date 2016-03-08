(ns durable.aabuffer
  (:require [octet.core :as buf])
  #?(:clj (:import (java.nio CharBuffer ByteBuffer))))

(defprotocol aa-buffer
  (-capacity [this])
  (-position [this])
  (-position! [this np])
  (-limit [this])
  (-limit! [this nl])
  (-clear! [this])
  (-flip! [this])
  (-rewind! [this])
  (-remaining [this])
  (-remaining? [this])
  (-mark! [this])
  (-revert! [this]))

#?(:clj (extend-type ByteBuffer
          aa-buffer
          (-capacity [this] (.capacity this))
          (-position [this] (.position this))
          (-position! [this np] (.position this np))
          (-limit [this] (.limit this))
          (-limit! [this nl] (.limit this nl))
          (-clear! [this] (.clear this))
          (-flip! [this] (.flip this))
          (-rewind! [this] (.rewind this))
          (-remaining [this] (.remaining this))
          (-remaining? [this] (.hasRemaining this))
          (-mark! [this] (.mark this))
          (-revert! [this] (.reset this)))
   :cljs (deftype aabuf  [^{:volatile-mutable true} m
                          ^{:volatile-mutable true} p
                          ^{:volatile-mutable true} l
                          o c b]
           aa-buffer
           (-capacity [this] (aget b "byteLength"))
           (-position [this] p)
           (-position! [this np]
             (if (or (< np 0) (> np l))
               (throw "invalid position")
               (do
                 (set! p np)
                 (if (< p m)
                   (set! m -1))))
             b)
           (-limit [this] l)
           (-limit! [this nl]
             (if (> nl c)
               (throw "invalid limit")
               (do
                 (set! l nl)
                 (if (< l p)
                   (-position! this l))))
             b)
           (-clear! [this] (set! m -1) (set! p 0) (set! l c) b)
           (-flip! [this] (set! l p) (set! p 0) b)
           (-rewind! [this] (set! p 0) b)
           (-remaining [this] (- l p))
           (-remaining? [this] (< p l))
           (-mark! [this] (set! m p))
           (-revert! [this]
             (if (= m -1)
               (throw "invalid mark")
               (set! p m)))))

(defn newBuffer [size]
#?(:clj (buf/allocate size)
   :cljs (->aabuf -1 0 size 0 size (buf/allocate size))))
