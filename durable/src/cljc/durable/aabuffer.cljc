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
  (-flip! [this]))

#?(:clj (extend-type ByteBuffer
          aa-buffer
          (-capacity [this] (.capacity this))
          (-position [this] (.position this))
          (-position! [this np] (.position this np))
          (-limit [this] (.limit this))
          (-limit! [this nl] (.limit this nl))
          (-clear! [this] (.clear this))
          (-flip! [this] (.flip this)))
   :cljs (deftype aabuf  [^{:volatile-mutable true} p ^{:volatile-mutable true} l b]
           aa-buffer
           (-capacity [this] (aget b "byteLength"))
           (-position [this] p)
           (-position! [this np] (set! p np))
           (-limit [this] l)
           (-limit! [this nl] (set! l nl))
           (-clear! [this] (set! p 0) (set! l (-capacity this)))
           (-flip! [this] (set! l p) (set! p 0))))

(defn newBuffer [size]
#?(:clj (buf/allocate size)
   :cljs (->aabuf 0 size (buf/allocate size))))
