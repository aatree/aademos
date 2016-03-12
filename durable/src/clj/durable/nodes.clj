(ns durable.nodes
  (:require   [durable.base :as base]
              [aautil.buffer :as buffer]
              [octet.core :as spec])
  (:import (clojure.lang Counted)
           (java.util Iterator)
           (durable CountedSequence)))

(set! *warn-on-reflection* true)

(defprotocol INoded
  (-getState [this]))

(defprotocol INode
  (-newNode [this t2 ^Long level left right ^Long cnt opts])
  (-getT2 [this opts])
  (^Long -getLevel [this opts])
  (-getLeft [this opts])
  (-getRight [this opts])
  (^Long -getCnt [this opts])
  (-getNada [this]))

(defprotocol WrapperNode
  (-svalAtom [this])
  (-blenAtom [this])
  (-bufferAtom [this])
  (-factory [this])
  (-nodeByteLength [this opts])
  (-nodeWrite [this buffer opts]))

(defprotocol AAContext
  (-classAtom [this])
  (-getDefaultFactory [this])
  (-setDefaultFactory [this factory])
  (-refineInstance [this inst]))

(defprotocol IFactory
  (-factoryId [this])
  (-instanceClass [this])
  (-qualified [this t2 opts])
  (-sval [this inode opts])
  (-valueLength [this node opts])
  (-deserialize [this node buffer opts])
  (-writeValue [this node buffer opts])
  (-valueNode [this node opts]))

(deftype noded-state [node opts meta])

(defn ^noded-state get-state [this]
  (-getState this))

(defn get-inode [noded]
  (.-node (get-state noded)))

(defn get-opts [noded]
  (.-opts (get-state noded)))

(defn get-meta [noded]
  (.-meta (get-state noded)))

(defn empty-node? [n]
  (or (nil? n) (identical? n (-getNada n))))

(defn last-t2 [this opts]
  (cond
    (empty-node? this)
    nil
    (empty-node? (-getRight this opts))
    (-getT2 this opts)
    :else
    (recur (-getRight this opts) opts)))

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

(defn ^Long node-count [this opts]
  (if (empty-node? this)
    0
    (-getCnt this opts)))

(defn revise [this args opts]
  (let [m (apply array-map args)
        t-2 (get m :t2 (-getT2 this opts))
        ^Long lev (get m :level (-getLevel this opts))
        l (get m :left (left-node this opts))
        r (get m :right (right-node this opts))
        ^Long c (+ 1 (node-count l opts) (node-count r opts))]
    (if (and (identical? t-2 (-getT2 this opts))
             (= lev (-getLevel this opts))
             (identical? l (left-node this opts))
             (identical? r (right-node this opts)))
      this
      (-newNode this t-2 lev l r c opts))))

(defn skew
  [this opts]
  (cond
    (empty-node? this)
    this
    (empty-node? (-getLeft this opts))
    this
    (= (-getLevel (left-node this opts) opts) (-getLevel this opts))
    (let [l (-getLeft this opts)]
      (revise l [:right (revise this [:left (right-node l opts)] opts)] opts))
    :else
    this))

(defn split [this opts]
  (cond
    (empty-node? this)
    this
    (or (empty-node? (right-node this opts))
        (empty-node? (right-node (right-node this opts) opts)))
    this
    (= (-getLevel this opts) (-getLevel (right-node (right-node this opts) opts) opts))
    (revise (right-node this opts)
            [:level (+ 1 (-getLevel (right-node this opts) opts))
             :left (revise this [:right (-getLeft (right-node this opts) opts)] opts)]
            opts)
    :else
    this))

(defn predecessor-t2 [this opts]
  (last-t2 (left-node this opts) opts))

(defn decrease-level [this opts]
  (let [should-be (+ 1 (min (-getLevel (left-node this opts) opts)
                            (-getLevel (right-node this opts) opts)))]
    (if (>= should-be (-getLevel this opts))
      this
      (let [rn (right-node this opts)
            rn (if (>= should-be (-getLevel (right-node this opts) opts))
                 rn
                 (revise rn [:level should-be] opts))]
        (revise this [:right rn :level should-be] opts)))))

(defn nth-t2 [this i opts]
  (if (empty-node? this)
    (throw (IndexOutOfBoundsException.))
    (let [l (left-node this opts)
          p (-getCnt l opts)]
      (cond
        (< i p)
        (nth-t2 l i opts)
        (> i p)
        (nth-t2 (right-node this opts) (- i p 1) opts)
        :else
        (-getT2 this opts)))))

(defn deln [this i opts]
  (if (empty-node? this)
    this
    (let [l (left-node this opts)
          p (-getCnt l opts)]
      (if (and (= i p) (= 1 (-getLevel this opts)))
        (right-node this opts)
        (let [t (cond
                  (> i p)
                  (revise this [:right (deln (right-node this opts) (- i p 1) opts)] opts)
                  (< i p)
                  (revise this [:left (deln (left-node this opts) i opts)] opts)
                  :else
                  (let [pre (predecessor-t2 this opts)]
                    (revise this [:t2 pre :left (deln (left-node this opts) (- i 1) opts)] opts)))
              t (decrease-level t opts)
              t (skew t opts)
              t (revise t [:right (skew (right-node t opts) opts)] opts)
              r (right-node t opts)
              t (if (empty-node? r)
                  t
                  (revise t [:right (revise r [:right (skew (right-node r opts) opts)] opts)] opts))
              t (split t opts)
              t (revise t [:right (split (right-node t opts) opts)] opts)]
          t)))))

(deftype counted-iterator
  [node
   ^{:volatile-mutable true} ndx
   ^Long cnt
   opts]

  base/XIterator
  (xicount [this index]
    (- cnt index))
  (xiindex [this]
    ndx)
  (xibumpIndex [this index]
    (+ 1 index))
  (xifetch [this index]
    (nth-t2 node index opts))

  Counted
  (count [this]
    (base/xicount this ndx))

  Iterator
  (hasNext [this]
    (< ndx cnt))
  (next [this]
    (let [i ndx]
      (set! ndx (base/xibumpIndex this i))
      (base/xifetch this i))))

(defn ^counted-iterator new-counted-iterator
  ([node opts]
   (->counted-iterator node 0 (-getCnt node opts) opts))
  ([node i opts]
   (->counted-iterator node i (-getCnt node opts) opts)))

(defn create-counted-sequence [iter initialIndex styp]
  (CountedSequence/create iter initialIndex styp))

(defn new-counted-seq
  ([node opts]
   (let [it (new-counted-iterator node opts)]
     (create-counted-sequence it (base/xiindex it) identity)))
  ([node i opts]
   (let [it (new-counted-iterator node i opts)]
     (create-counted-sequence it (base/xiindex it) identity))))

(deftype counted-reverse-iterator
  [node
   ^{:volatile-mutable true} ndx
   opts]

  base/XIterator
  (xicount [this index]
    (+ 1 index))
  (xiindex [this]
    ndx)
  (xibumpIndex [this index]
    (- index 1))
  (xifetch [this index]
    (nth-t2 node index opts))

  Counted
  (count [this]
    (base/xicount this ndx))

  Iterator
  (hasNext [this]
    (>= ndx 0))
  (next [this]
    (let [i ndx]
      (set! ndx (base/xibumpIndex this i))
      (base/xifetch this i))))

(defn ^counted-reverse-iterator new-counted-reverse-iterator
  ([node opts]
   (->counted-reverse-iterator node (- (-getCnt node opts) 1) opts))
  ([node i opts]
   (->counted-reverse-iterator node i opts)))

(defn new-counted-reverse-seq
  ([node opts]
   (let [it (new-counted-reverse-iterator node opts)]
     (create-counted-sequence it (base/xiindex it) identity)))
  ([node i opts]
   (let [it (new-counted-reverse-iterator node i opts)]
     (create-counted-sequence it (base/xiindex it) identity))))

(defn vector-add [n v i opts]
  (if (empty-node? n)
    (-newNode n v 1 nil nil 1 opts)
    (let [l (left-node n opts)
          p (-getCnt l opts)]
      (split
        (skew
          (if (<= i p)
            (revise n [:left (vector-add l v i opts)] opts)
            (revise n [:right (vector-add (right-node n opts) v (- i p 1) opts)] opts))
          opts)
        opts))))

(defn vector-set [n v i opts]
  (if (empty-node? n)
    (-newNode n v 1 nil nil 1 opts)
    (let [l (left-node n opts)
          p (-getCnt l opts)]
      (split
        (skew
          (cond
            (< i p)
            (revise n [:left (vector-set l v i opts)] opts)
            (> i p)
            (revise n [:right (vector-set (right-node n opts) v (- i p 1) opts)] opts)
            :else
            (revise n [:t2 v] opts))
          opts)
        opts))))

(defn get-entry [this opts] (-getT2 this opts))

(defn key-of [e] (key e))

(defn value-of [e] (val e))

(defn map-cmpr [this x comparator opts]
  (comparator x (key (get-entry this opts))))

(defn resource-cmpr [this x opts] (map-cmpr this x (:comparator opts) opts))

(defn map-index-of [this x opts]
  (if (empty-node? this)
    0
    (let [c (resource-cmpr this x opts)]
      (cond
        (< c 0)
        (map-index-of (left-node this opts) x opts)
        (= c 0)
        (-getCnt (left-node this opts) opts)
        :else
        (+ 1
           (-getCnt (left-node this opts) opts)
           (map-index-of (right-node this opts) x opts))))))

(defn ^counted-iterator new-map-entry-iterator
  ([node x opts]
   (->counted-iterator node (map-index-of node x opts) (-getCnt node opts) opts)))

(defn new-map-entry-seq
  ([node x opts]
   (let [it (new-map-entry-iterator node x opts)]
     (create-counted-sequence it (base/xiindex it) identity))))

(defn new-map-key-seq [node opts]
  (let [it (new-counted-iterator node opts)]
    (create-counted-sequence it (base/xiindex it) key-of)))

(defn new-map-value-seq [node opts]
  (let [it (new-counted-iterator node opts)]
    (create-counted-sequence it (base/xiindex it) value-of)))

(defn ^counted-reverse-iterator new-map-entry-reverse-iterator
  ([node x opts]
   (->counted-reverse-iterator node (map-index-of node x opts) opts)))

(defn new-map-entry-reverse-seq
  ([node x opts]
   (let [it (new-map-entry-reverse-iterator node x opts)]
     (create-counted-sequence it (base/xiindex it) identity))))

(defn new-map-key-reverse-seq [node opts]
  (let [it (new-counted-reverse-iterator node opts)]
    (create-counted-sequence it (base/xiindex it) key-of)))

(defn new-map-value-reverse-seq [node opts]
  (let [it (new-counted-reverse-iterator node opts)]
    (create-counted-sequence it (base/xiindex it) value-of)))

(defn map-insert [this t-2 opts]
  (if (empty-node? this)
    (-newNode this t-2 1 nil nil 1 opts)
    (let [c (resource-cmpr this (key t-2) opts)]
      (split (skew (cond
                     (< c 0)
                     (let [oldl (left-node this opts)
                           l (map-insert oldl t-2 opts)]
                       (revise this [:left l] opts))
                     (> c 0)
                     (let [oldr (right-node this opts)
                           r (map-insert oldr t-2 opts)]
                       (revise this [:right r] opts))
                     :else
                     (if (identical? (val t-2) (val (get-entry this opts)))
                       this
                       (revise this
                               [:t2 (base/newMapEntry (key (get-entry this opts)) (val t-2))]
                               opts))) opts) opts))))

(defn map-get-t2 [this x opts]
  (if (empty-node? this)
    nil
    (let [c (resource-cmpr this x opts)]
      (cond
        (zero? c) (-getT2 this opts)
        (> c 0) (map-get-t2 (right-node this opts) x opts)
        :else (map-get-t2 (left-node this opts) x opts)))))

(defn map-del [this x opts]
  (if (empty-node? this)
    this
    (let [c (resource-cmpr this x opts)]
      (if (and (= c 0) (= 1 (-getLevel this opts)))
        (right-node this opts)
        (let [t (cond
                  (> c 0)
                  (revise this [:right (map-del (right-node this opts) x opts)] opts)
                  (< c 0)
                  (revise this [:left (map-del (left-node this opts) x opts)] opts)
                  :else
                  (let [p (predecessor-t2 this opts)]
                    (revise this [:t2 p :left (map-del (left-node this opts) (key p) opts)] opts)))
              t (decrease-level t opts)
              t (skew t opts)
              t (revise t [:right (skew (right-node t opts) opts)] opts)
              r (right-node t opts)
              t (if (empty-node? r)
                  t
                  (revise t [:right (revise r [:right (skew (right-node r opts) opts)] opts)] opts))
              t (split t opts)
              t (revise t [:right (split (right-node t opts) opts)] opts)]
          t)))))

(declare ->Node
         create-empty-node)

(deftype Node [t2 ^Long level left right ^Long cnt]

  INode

  (-newNode [this t2 level left right cnt opts]
    (->Node t2 level left right cnt))

  (-getT2 [this opts] t2)

  (-getLevel [this opts] level)

  (-getLeft [this opts] left)

  (-getRight [this opts] right)

  (-getCnt [this opts] cnt)

  (-getNada [this] (create-empty-node)))

(def emptyNode
  (->Node nil 0 nil nil 0))

(defn create-empty-node []
  emptyNode)

(defn snodev [this opts]
  (if (empty-node? this)
    ""
    (str (snodev (-getLeft this opts) opts)
         " <"
         (-getT2 this opts)
         " "
         (-getLevel this opts)
         "> "
         (snodev (-getRight this opts) opts))))

(defn pnodev [this dsc opts]
  (println dsc (snodev this opts)))

(deftype factory-registry [by-id-atom by-class-atom])

(defn ^factory-registry create-factory-registry
  ([]
   (factory-registry. (atom {})
                      (atom {})))
  ([^factory-registry fregistry]
   (factory-registry. (atom @(.-by_id_atom fregistry))
                      (atom @(.by_class_atom fregistry)))))

(def default-factory-registry (create-factory-registry))

(defn factory-for-id [id opts]
  (let [^factory-registry r (:factory-registry opts)
        _ (if (nil? r) (println "oh!"))
        f (@(.-by_id_atom r) id)]
    (if (nil? f)
      (let [context (:aacontext opts)]
        (-getDefaultFactory context))
      f)))

(defn register-class [aacontext factory]
  (let [clss (-instanceClass factory)]
    (if clss
      (swap! (-classAtom aacontext) assoc clss factory))))

(defn factory-for-class [aacontext clss opts]
  (let [f (@(-classAtom aacontext) clss)]
    (if (nil? f)
      (let [context (:aacontext opts)]
        (-getDefaultFactory context))
      f)))

(defn className [^Class c] (.getName c))

(defn factory-for-instance [inst opts]
  (let [aacontext (:aacontext opts)
        inst (-refineInstance aacontext inst)
        clss (class inst)
        f (factory-for-class aacontext clss opts)
        q (-qualified f inst opts)]
    (if (nil? q)
      (throw (UnsupportedOperationException. (str "Unknown qualified durable class: " (className clss))))
      q)))

(defn register-factory [^factory-registry fregistry
                        aacontext
                        factory]
  (swap! (.-by-id-atom fregistry) assoc (-factoryId factory) factory)
  (register-class aacontext factory))

(defn node-byte-length [wrapper-node opts]
  (-nodeByteLength wrapper-node opts))

(defn node-write [wrapper-node buffer opts]
  (-nodeWrite wrapper-node buffer opts))

(defn get-factory [wrapper-node]
  (-factory wrapper-node))

(defn get-buffer-atom [wrapper-node]
  (-bufferAtom wrapper-node))

(defn get-buffer [wrapper-node]
  @(-bufferAtom wrapper-node))

(defn str-val [factory wrapper-node opts]
  (let [sval-atom (-svalAtom wrapper-node)]
    (if (nil? @sval-atom)
      (compare-and-set! sval-atom nil (-sval factory wrapper-node opts)))
    @sval-atom))

(defn default-sval [this inode opts]
  (pr-str (-getT2 inode opts)))

(defn key-sval [this inode opts]
  (let [map-entry (-getT2 inode opts)]
    (pr-str (key map-entry))))

(defn deserialize-sval [this wrapper-node bb opts]
  (let [sv (buffer/-read! bb spec/string*)]
    (reset! (-svalAtom wrapper-node) sv)
    (read-string opts sv)))

(defn default-valueLength [this wrapper-node opts]
  (+ 4                                                      ;sval length
     (* 2 (count (str-val this wrapper-node opts)))))       ;sval

(defn default-write-value [f
                           wrapper-node
                           buffer
                           opts]
  (let [^String sv (str-val f wrapper-node opts)]
    (buffer/-write! buffer sv spec/string*)))

(def vector-context
  (let [class-atom (atom {})
        factory-atom (atom nil)]
    (reify AAContext
      (-classAtom [this] class-atom)
      (-getDefaultFactory [this] @factory-atom)
      (-setDefaultFactory
        [this f]
        (compare-and-set! factory-atom nil f))
      (-refineInstance [this inst] inst))))

(def map-context
  (let [class-atom (atom {})
        factory-atom (atom nil)]
    (reify AAContext
      (-classAtom [this] class-atom)
      (-getDefaultFactory [this] @factory-atom)
      (-setDefaultFactory
        [this f]
        (compare-and-set! factory-atom nil f))
      (-refineInstance [this inst]
        (let [map-entry inst]
          (val map-entry))))))

(def set-context
  (let [class-atom (atom {})
        factory-atom (atom nil)]
    (reify AAContext
      (-classAtom [this] class-atom)
      (-getDefaultFactory [this] @factory-atom)
      (-setDefaultFactory
        [this f]
        (compare-and-set! factory-atom nil f))
      (-refineInstance [this inst]
        (let [map-entry inst]
          (key map-entry))))))

(defn vector-opts [opts]
  (assoc opts :aacontext vector-context))

(defn map-opts [opts]
  (assoc opts :aacontext map-context))

(defn set-opts [opts]
  (assoc opts :aacontext set-context))

(defn node-read [buffer opts]
  ((:node-read opts) buffer opts))

(register-factory
  default-factory-registry
  nil
  (reify IFactory
    (-factoryId [this] (byte \n))                            ;;;;;;;;;;;;;;;;;;;;;;;; n - nil content
    (-instanceClass [this] nil)
    (-qualified [this t2 opts] this)
    (-valueNode [this node opts] nil)))

(register-factory
  default-factory-registry
  vector-context
  (reify IFactory
    (-factoryId [this] (byte \e))                            ;;;;;;;;;;;;;;;;;;;;;; e - vector default factory
    (-instanceClass [this] nil)
    (-qualified [this t2 opts] this)
    (-sval [this inode opts]
      (default-sval this inode opts))
    (-valueLength [this node opts]
      (default-valueLength this node opts))
    (-deserialize [this node bb opts]
      (deserialize-sval this node bb opts))
    (-writeValue [this node buffer opts]
      (default-write-value this node buffer opts))
    (-valueNode [this node opts] nil)))

(-setDefaultFactory
  vector-context
  (factory-for-id
    (byte \e)
    {:factory-registry default-factory-registry}))

(register-factory
  default-factory-registry
  map-context
  (reify IFactory
    (-factoryId [this] (byte \p))                            ;;;;;;;;;;;;;;;;;;;;;;;;;;; p - map default factory
    (-instanceClass [this] nil)
    (-qualified [this t2 opts] this)
    (-sval [this inode opts]
      (default-sval this inode opts))
    (-valueLength [this node opts]
      (default-valueLength this node opts))
    (-deserialize [this node bb opts]
      (let [v (deserialize-sval this node bb opts)
            t2 (base/newMapEntry (get v 0) (get v 1))]
        t2))
    (-writeValue [this node buffer opts]
      (default-write-value this node buffer opts))
    (-valueNode [this node opts] nil)))

(-setDefaultFactory
  map-context
  (factory-for-id
    (byte \p)
    {:factory-registry default-factory-registry}))

(register-factory
  default-factory-registry
  set-context
  (reify IFactory
    (-factoryId [this] (byte \q))                            ;;;;;;;;;;;;;;;;;;;;;;;;;;; q - set default factory
    (-instanceClass [this] nil)
    (-qualified [this t2 opts] this)
    (-sval [this inode opts]
      (key-sval this inode opts))
    (-valueLength [this node opts]
      (default-valueLength this node opts))
    (-deserialize [this node bb opts]
      (let [k (deserialize-sval this node bb opts)]
        (base/newMapEntry k k)))
    (-writeValue [this node buffer opts]
      (default-write-value this node buffer opts))
    (-valueNode [this node opts] nil)))

(-setDefaultFactory
  set-context
  (factory-for-id
    (byte \q)
    {:factory-registry default-factory-registry}))

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
