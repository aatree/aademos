(ns durable.aabuffer
  (:require [octet.core :as buf])
  #?(:clj (:import (java.nio CharBuffer ByteBuffer))))

(defprotocol aa-buffer
  (-capacity [this])
  (-position [this]))

#?(:clj (extend-type ByteBuffer
          aa-buffer
          (-capacity [this] (.capacity this))
          (-position [this] (.position this)))
   :cljs (deftype aabuf  [p b]
           aa-buffer
           (-capacity [this] (aget b "byteLength"))
           (-position [this] p)))

(defn newBuffer [size]
#?(:clj (buf/allocate size)
   :cljs (->aabuf 0 (buf/allocate size))))
