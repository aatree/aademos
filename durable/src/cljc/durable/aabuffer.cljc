(ns durable.aabuffer
  (:require [octet.core :as buf])
  #?(:clj (:import (java.nio CharBuffer ByteBuffer))))

(defprotocol buffer
  )

#?(:clj (extend-type ByteBuffer
          buffer)
   :cljs (deftype aabuf  [p b]
           buffer))

(defn newBuffer [size]
#?(:clj (buf/allocate size)
   :cljs (->aabuf 0 (buf/allocate size))))
