(ns durable.base
  #?(:clj (:import (clojure.lang MapEntry))))

#?(:clj (set! *warn-on-reflection* true))

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
  (-nth-t2 [this i opts])
  (-split [this opts])
  (-skew [this opts])
  (-revise [this args opts])
  (-predecessor-t2 [this opts])
  (-decrease-level [this opts]))

#?(:clj (defn newMapEntry [k v] (MapEntry. k v)))

#?(:cljs (defn newMapEntry [k v] [k v]))

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

(defn vector-add [n v i opts]
  (if (empty-node? n)
    (-newNode n v 1 nil nil 1 opts)
    (let [l (left-node n opts)
          p (-getCnt l opts)]
      (-split
        (-skew
          (if (<= i p)
            (-revise n [:left (vector-add l v i opts)] opts)
            (-revise n [:right (vector-add (right-node n opts) v (- i p 1) opts)] opts))
          opts)
        opts))))

(defn deln [this i opts]
  (if (empty-node? this)
    this
    (let [l (left-node this opts)
          p (-getCnt l opts)]
      (if (and (= i p) (= 1 (-getLevel this opts)))
        (right-node this opts)
        (let [t (cond
                  (> i p)
                  (-revise this [:right (deln (right-node this opts) (- i p 1) opts)] opts)
                  (< i p)
                  (-revise this [:left (deln (left-node this opts) i opts)] opts)
                  :else
                  (let [pre (-predecessor-t2 this opts)]
                    (-revise this [:t2 pre :left (deln (left-node this opts) (- i 1) opts)] opts)))
              t (-decrease-level t opts)
              t (-skew t opts)
              t (-revise t [:right (-skew (right-node t opts) opts)] opts)
              r (right-node t opts)
              t (if (empty-node? r)
                  t
                  (-revise t [:right (-revise r [:right (-skew (right-node r opts) opts)] opts)] opts))
              t (-split t opts)
              t (-revise t [:right (-split (right-node t opts) opts)] opts)]
          t)))))
