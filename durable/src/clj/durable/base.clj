(ns durable.base
  (:import (clojure.lang MapEntry)))

(set! *warn-on-reflection* true)

(defprotocol XIterator
  (^Long xiindex [this])
  (xibumpIndex [this index])
  (xicount [this index])
  (xifetch [this index]))

(defprotocol INoded
  (-getState [this]))

(defprotocol INode
  (-newNode [this t2 level left right cnt opts])
  (-getT2 [this opts])
  (-getLevel [this opts])
  (-getLeft [this opts])
  (-getRight [this opts])
  (-getCnt [this opts])
  (-getNada [this])
  (-new-counted-iterator [this opts])
  (-new-counted-seq [this opts])
  )

(defn newMapEntry [k v] (MapEntry. k v))

(deftype noded-state [node opts meta])

(defn ^noded-state get-state [this]
  (-getState this))

(defn get-inode [noded]
  (.-node (get-state noded)))

(defn get-opts [noded]
  (.-opts (get-state noded)))

(defn get-meta [noded]
  (.-meta (get-state noded)))

(defn same? [val opts]
  (if (instance? INoded val)
    (let [vopts (get-opts val)]
      (if (and (= (:new-vector opts) (:new-vector vopts))
               (= (:db-file opts) (:db-file vopts)))
        true
        false))
    false))

(defn transcribe-vector [val opts]
  (reduce conj ((:new-vector opts) opts) (seq val)))

(defn transcribe-sorted-map [val opts]
  (reduce conj ((:new-sorted-map opts) opts) (seq val)))

(defn transcribe-sorted-set [val opts]
  (reduce conj ((:new-sorted-set opts) opts) (seq val)))

(defn transcriber [val opts]
  (if (list? val)
    (if (vector? val)
      (if (same? val opts)
        val
        (transcribe-vector val opts))
      val)
    (if (map? val)
      (if (same? val opts)
        val
        (transcribe-sorted-map val opts))
      (if (set? val)
        (if (same? val opts)
          val
          (transcribe-sorted-set val opts))
        val))))

(defn empty-node? [n]
  (or (nil? n) (identical? n (-getNada n))))

(defn empty-node [this opts]
  (if (empty-node? this)
    this
    (-getNada this)))

(defn left-node [this opts]
  (if (empty-node? (-getLeft this opts))
    (empty-node this opts)
    (-getLeft this opts)))

(defn right-node [this opts]
  (if (empty-node? (-getRight this opts))
    (empty-node this opts)
    (-getRight this opts)))

(defprotocol FlexVector
  (-dropNode [this i])
  (-addNode [this i v]))
