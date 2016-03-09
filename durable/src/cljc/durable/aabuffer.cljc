(ns durable.aabuffer
  (:require [octet.core :as buf]
            [octet.spec :as spec])
  (:refer-clojure :exclude [-reset!])
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
  (-reset! [this])
  (-write!
    [this data spec])
  (-write-at!
    [this data spec offset])
  (-read! [this spec])
  (-read-at [this spec offset]))

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
          (-reset! [this] (.reset this))
          (-write!
            [this data spec]
            (let [p (.position this)
                  dl (spec/write spec this p data)]
              (.position this (+ p dl))
              dl))
          (-write-at!
            [this data spec offset] (spec/write spec this offset data))
          (-read!
            [this spec]
            (let [p (.position this)
                  [dl data] (spec/read spec this p)]
              (.position this (+ p dl))
              data))
          (-read-at
            [this spec offset] (spec/read spec this offset)))
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
           (-reset! [this]
             (if (= m -1)
               (throw "invalid mark")
               (set! p m)))
           (-write!
             [this data spec]
             (let [dl (spec/write spec b p data)]
               (set! p (+ p dl))
               (when (> p l)
                 (set! p (- p dl))
                 (throw "possible buffer corruption by writing past limit"))
               dl))
           (-write-at!
             [this data spec offset]
             (let [dl (spec/write spec b offset data)
                   np (+ offset dl)]
               (if (> np l)
                 (throw "possible buffer corruption by writing past limit"))
               dl))
           (-read!
             [this spec]
             (let [[dl data] (spec/read spec b p)]
               (set! p (+ p dl))
               (when (> p l)
                 (set! p (- p dl))
                 (throw "read past limit"))
               data))
           (-read-at
             [this spec offset]
             (let [[dl data] (spec/read spec b offset)
                   np (+ offset dl)]
               (if (> np l)
                 (throw "read past limit"))
               data))))

(defn newBuffer [size]
#?(:clj (buf/allocate size)
   :cljs (->aabuf -1 0 size 0 size (buf/allocate size))))
