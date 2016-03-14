(ns durable.nodes
  #?(:clj
           (:require [durable.base :as base]
                     [aautil.buffer :as buffer]
                     [octet.core :as spec]
                     [clojure.string :as str]
                     [clojure.edn :refer [read-string]])
     :cljs (:require [durable.base :as base]
             [durable.CountedSequence :as CountedSequence]
             [aautil.buffer :as buffer]
             [octet.core :as spec]
             [clojure.string :as str]
             [cljs.reader :refer [read-string]]))
  #?(:clj
     (:refer-clojure :exclude [read-string]))
  #?(:clj (:import (clojure.lang Counted)
                   (java.util Iterator)
                   (durable CountedSequence))))

#?(:clj
   (set! *warn-on-reflection* true))

(defprotocol WrapperNode
  (-svalAtom [this])
  (-blenAtom [this])
  (-bufferAtom [this])
  (-factory [this])
  (-nodeByteLength [this opts])
  (-nodeWrite [this buffer opts]))

(defprotocol AAContext
  (-typeAtom [this])
  (-getDefaultFactory [this])
  (-setDefaultFactory [this factory])
  (-refineInstance [this inst]))

(defprotocol IFactory
  (-factoryId [this])
  (-instanceType [this])
  (-qualified [this t2 opts])
  (-sval [this inode opts])
  (-valueLength [this node opts])
  (-deserialize [this node buffer])
  (-writeValue [this node buffer opts])
  (-valueNode [this node opts]))

(defn last-t2 [this opts]
  (cond
    (base/empty-node? this)
    nil
    (base/empty-node? (base/-getRight this opts))
    (base/-getT2 this opts)
    :else
    (recur (base/-getRight this opts) opts)))

(defn node-count [this opts]
  (if (base/empty-node? this)
    0
    (base/-getCnt this opts)))

(defn revise [this args opts]
  (let [m (apply array-map args)
        t-2 (get m :t2 (base/-getT2 this opts))
        lev (get m :level (base/-getLevel this opts))
        l (get m :left (base/left-node this opts))
        r (get m :right (base/right-node this opts))
        c (+ 1 (node-count l opts) (node-count r opts))]
    (if (and (identical? t-2 (base/-getT2 this opts))
             (= lev (base/-getLevel this opts))
             (identical? l (base/left-node this opts))
             (identical? r (base/right-node this opts)))
      this
      (base/-newNode this t-2 lev l r c opts))))

(defn skew
  [this opts]
  (cond
    (base/empty-node? this)
    this
    (base/empty-node? (base/-getLeft this opts))
    this
    (= (base/-getLevel (base/left-node this opts) opts) (base/-getLevel this opts))
    (let [l (base/-getLeft this opts)]
      (revise l [:right (revise this [:left (base/right-node l opts)] opts)] opts))
    :else
    this))

(defn split [this opts]
  (cond
    (base/empty-node? this)
    this
    (or (base/empty-node? (base/right-node this opts))
        (base/empty-node? (base/right-node (base/right-node this opts) opts)))
    this
    (= (base/-getLevel this opts) (base/-getLevel (base/right-node (base/right-node this opts) opts) opts))
    (revise (base/right-node this opts)
            [:level (+ 1 (base/-getLevel (base/right-node this opts) opts))
             :left (revise this [:right (base/-getLeft (base/right-node this opts) opts)] opts)]
            opts)
    :else
    this))

(defn predecessor-t2 [this opts]
  (last-t2 (base/left-node this opts) opts))

(defn decrease-level [this opts]
  (let [should-be (+ 1 (min (base/-getLevel (base/left-node this opts) opts)
                            (base/-getLevel (base/right-node this opts) opts)))]
    (if (>= should-be (base/-getLevel this opts))
      this
      (let [rn (base/right-node this opts)
            rn (if (>= should-be (base/-getLevel (base/right-node this opts) opts))
                 rn
                 (revise rn [:level should-be] opts))]
        (revise this [:right rn :level should-be] opts)))))

(defn nth-t2 [this i opts]
  (if (base/empty-node? this)
    #?(:clj (throw (IndexOutOfBoundsException.))
       :cljs (throw "IndexOutOfBoundsException"))
    (let [l (base/left-node this opts)
          p (base/-getCnt l opts)]
      (cond
        (< i p)
        (nth-t2 l i opts)
        (> i p)
        (nth-t2 (base/right-node this opts) (- i p 1) opts)
        :else
        (base/-getT2 this opts)))))

(defn deln [this i opts]
  (if (base/empty-node? this)
    this
    (let [l (base/left-node this opts)
          p (base/-getCnt l opts)]
      (if (and (= i p) (= 1 (base/-getLevel this opts)))
        (base/right-node this opts)
        (let [t (cond
                  (> i p)
                  (revise this [:right (deln (base/right-node this opts) (- i p 1) opts)] opts)
                  (< i p)
                  (revise this [:left (deln (base/left-node this opts) i opts)] opts)
                  :else
                  (let [pre (predecessor-t2 this opts)]
                    (revise this [:t2 pre :left (deln (base/left-node this opts) (- i 1) opts)] opts)))
              t (decrease-level t opts)
              t (skew t opts)
              t (revise t [:right (skew (base/right-node t opts) opts)] opts)
              r (base/right-node t opts)
              t (if (base/empty-node? r)
                  t
                  (revise t [:right (revise r [:right (skew (base/right-node r opts) opts)] opts)] opts))
              t (split t opts)
              t (revise t [:right (split (base/right-node t opts) opts)] opts)]
          t)))))

(deftype counted-iterator
  [node ^{:volatile-mutable true} ndx cnt opts]

  #?@(:cljs (Object
              (hasNext [this]
                       (< ndx cnt))
              (next [this]
                    (let [i ndx]
                      (set! ndx (base/xibumpIndex this i))
                      (base/xifetch this i))))
      :clj (Iterator
             (hasNext [this]
               (< ndx cnt))
             (next [this]
               (let [i ndx]
                 (set! ndx (base/xibumpIndex this i))
                 (base/xifetch this i)))))

  base/XIterator
  (xicount [this index]
    (- cnt index))
  (xiindex [this]
    ndx)
  (xibumpIndex [this index]
    (+ 1 index))
  (xifetch [this index]
    (nth-t2 node index opts))

  #?@(:cljs(ICounted
             (-count [this]
                     (base/xicount this ndx)))
      :clj(Counted
            (count [this]
              (base/xicount this ndx)))))

(defn ^counted-iterator new-counted-iterator
  ([node opts]
   (->counted-iterator node 0 (base/-getCnt node opts) opts))
  ([node i opts]
   (->counted-iterator node i (base/-getCnt node opts) opts)))

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
  [node ^{:volatile-mutable true} ndx opts]

  #?@(:cljs (Object
              (hasNext [this]
                       (>= ndx 0))
              (next [this]
                    (let [i ndx]
                      (set! ndx (base/xibumpIndex this i))
                      (base/xifetch this i))))
      :clj (Iterator
             (hasNext [this]
               (>= ndx 0))
             (next [this]
               (let [i ndx]
                 (set! ndx (base/xibumpIndex this i))
                 (base/xifetch this i)))))

  base/XIterator
  (xicount [this index]
    (+ 1 index))
  (xiindex [this]
    ndx)
  (xibumpIndex [this index]
    (- index 1))
  (xifetch [this index]
    (let [v (nth-t2 node index opts)]
      v
    ))

  #?@(:cljs(ICounted
             (-count [this]
                     (base/xicount this ndx)))
      :clj(Counted
            (count [this]
              (base/xicount this ndx)))))

(defn ^counted-reverse-iterator new-counted-reverse-iterator
  ([node opts]
   (->counted-reverse-iterator node (- (base/-getCnt node opts) 1) opts))
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
  (if (base/empty-node? n)
    (base/-newNode n v 1 nil nil 1 opts)
    (let [l (base/left-node n opts)
          p (base/-getCnt l opts)]
      (split
        (skew
          (if (<= i p)
            (revise n [:left (vector-add l v i opts)] opts)
            (revise n [:right (vector-add (base/right-node n opts) v (- i p 1) opts)] opts))
          opts)
        opts))))

(defn vector-set [n v i opts]
  (if (base/empty-node? n)
    (base/-newNode n v 1 nil nil 1 opts)
    (let [l (base/left-node n opts)
          p (base/-getCnt l opts)]
      (split
        (skew
          (cond
            (< i p)
            (revise n [:left (vector-set l v i opts)] opts)
            (> i p)
            (revise n [:right (vector-set (base/right-node n opts) v (- i p 1) opts)] opts)
            :else
            (revise n [:t2 v] opts))
          opts)
        opts))))

(defn get-entry [this opts] (base/-getT2 this opts))

(defn key-of [e] (key e))

(defn value-of [e] (val e))

(defn map-cmpr [this x comparator opts]
  (comparator x (key (get-entry this opts))))

(defn resource-cmpr [this x opts] (map-cmpr this x (:comparator opts) opts))

(defn map-index-of [this x opts]
  (if (base/empty-node? this)
    0
    (let [c (resource-cmpr this x opts)]
      (cond
        (< c 0)
        (map-index-of (base/left-node this opts) x opts)
        (= c 0)
        (base/-getCnt (base/left-node this opts) opts)
        :else
        (+ 1
           (base/-getCnt (base/left-node this opts) opts)
           (map-index-of (base/right-node this opts) x opts))))))

(defn ^counted-iterator new-map-entry-iterator
  ([node x opts]
   (->counted-iterator node (map-index-of node x opts) (base/-getCnt node opts) opts)))

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
  (if (base/empty-node? this)
    (base/-newNode this t-2 1 nil nil 1 opts)
    (let [c (resource-cmpr this (key t-2) opts)]
      (split (skew (cond
                     (< c 0)
                     (let [oldl (base/left-node this opts)
                           l (map-insert oldl t-2 opts)]
                       (revise this [:left l] opts))
                     (> c 0)
                     (let [oldr (base/right-node this opts)
                           r (map-insert oldr t-2 opts)]
                       (revise this [:right r] opts))
                     :else
                     (if (identical? (val t-2) (val (get-entry this opts)))
                       this
                       (revise this
                               [:t2 (base/newMapEntry (key (get-entry this opts)) (val t-2))]
                               opts))) opts) opts))))

(defn map-get-t2 [this x opts]
  (if (base/empty-node? this)
    nil
    (let [c (resource-cmpr this x opts)]
      (cond
        (zero? c) (base/-getT2 this opts)
        (> c 0) (map-get-t2 (base/right-node this opts) x opts)
        :else (map-get-t2 (base/left-node this opts) x opts)))))

(defn map-del [this x opts]
  (if (base/empty-node? this)
    this
    (let [c (resource-cmpr this x opts)]
      (if (and (= c 0) (= 1 (base/-getLevel this opts)))
        (base/right-node this opts)
        (let [t (cond
                  (> c 0)
                  (revise this [:right (map-del (base/right-node this opts) x opts)] opts)
                  (< c 0)
                  (revise this [:left (map-del (base/left-node this opts) x opts)] opts)
                  :else
                  (let [p (predecessor-t2 this opts)]
                    (revise this [:t2 p :left (map-del (base/left-node this opts) (key p) opts)] opts)))
              t (decrease-level t opts)
              t (skew t opts)
              t (revise t [:right (skew (base/right-node t opts) opts)] opts)
              r (base/right-node t opts)
              t (if (base/empty-node? r)
                  t
                  (revise t [:right (revise r [:right (skew (base/right-node r opts) opts)] opts)] opts))
              t (split t opts)
              t (revise t [:right (split (base/right-node t opts) opts)] opts)]
          t)))))

(declare ->Node
         create-empty-node)

(deftype Node [t2 level left right cnt]

  base/INode

  (-newNode [this t2 level left right cnt opts]
    (->Node t2 level left right cnt))

  (-getT2 [this opts] t2)

  (-getLevel [this opts] level)

  (-getLeft [this opts] left)

  (-getRight [this opts] right)

  (-getCnt [this opts] cnt)

  (-getNada [this] (create-empty-node))

  (-new-counted-iterator [this opts] (new-counted-iterator this opts))

  (-new-counted-seq [this opts] (new-counted-seq this opts))
  )

(def emptyNode
  (->Node nil 0 nil nil 0))

(defn create-empty-node []
  emptyNode)

(defn snodev [this opts]
  (if (base/empty-node? this)
    ""
    (str (snodev (base/-getLeft this opts) opts)
         " <"
         (base/-getT2 this opts)
         " "
         (base/-getLevel this opts)
         "> "
         (snodev (base/-getRight this opts) opts))))

(defn pnodev [this dsc opts]
  (println dsc (snodev this opts)))

(deftype factory-registry [by-id-atom by-type-atom])

(defn ^factory-registry create-factory-registry
  ([]
   (factory-registry. (atom {})
                      (atom {})))
  ([^factory-registry fregistry]
   (factory-registry. (atom @(.-by_id_atom fregistry))
                      (atom @(.by_type_atom fregistry)))))

(def default-factory-registry (create-factory-registry))

(defn factory-for-id [id opts]
  (let [^factory-registry r (:factory-registry opts)
        _ (if (nil? r) (println "oh!"))
        f (@(.-by_id_atom r) id)]
    (if (nil? f)
      (let [context (:aacontext opts)]
        (-getDefaultFactory context))
      f)))

(defn register-type [aacontext factory]
  (let [clss (-instanceType factory)]
    (if clss
      (swap! (-typeAtom aacontext) assoc clss factory))))

(defn factory-for-type [aacontext clss opts]
  (let [f (@(-typeAtom aacontext) clss)]
    (if (nil? f)
      (let [context (:aacontext opts)]
        (-getDefaultFactory context))
      f)))

(defn typeName [t] (str t))

(defn factory-for-instance [inst opts]
  (let [aacontext (:aacontext opts)
        inst (-refineInstance aacontext inst)
        clss (type inst)
        f (factory-for-type aacontext clss opts)
        q (-qualified f inst opts)]
    (if (nil? q)
      (let [m (str "Unknown qualified durable type: " (typeName clss))]
        #?(:clj (throw (UnsupportedOperationException. m))
           :cljs (throw (str "UnsupportedOperationException " m))))
      q)))

(defn register-factory [^factory-registry fregistry
                        aacontext
                        factory]
  (swap! (.-by-id-atom fregistry) assoc (-factoryId factory) factory)
  (register-type aacontext factory))

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
  (pr-str (base/-getT2 inode opts)))

(defn key-sval [this inode opts]
  (let [map-entry (base/-getT2 inode opts)]
    (pr-str (key map-entry))))

(defn deserialize-sval [this wrapper-node bb]
  (let [sv (buffer/-read! bb spec/string*)]
    (reset! (-svalAtom wrapper-node) sv)
    (read-string sv)))

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
  (let [type-atom (atom {})
        factory-atom (atom nil)]
    (reify AAContext
      (-typeAtom [this] type-atom)
      (-getDefaultFactory [this] @factory-atom)
      (-setDefaultFactory
        [this f]
        (compare-and-set! factory-atom nil f))
      (-refineInstance [this inst] inst))))

(def map-context
  (let [type-atom (atom {})
        factory-atom (atom nil)]
    (reify AAContext
      (-typeAtom [this] type-atom)
      (-getDefaultFactory [this] @factory-atom)
      (-setDefaultFactory
        [this f]
        (compare-and-set! factory-atom nil f))
      (-refineInstance [this inst]
        (let [map-entry inst]
          (val map-entry))))))

(def set-context
  (let [type-atom (atom {})
        factory-atom (atom nil)]
    (reify AAContext
      (-typeAtom [this] type-atom)
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
    (-instanceType [this] nil)
    (-qualified [this t2 opts] this)
    (-valueNode [this node opts] nil)))

(register-factory
  default-factory-registry
  vector-context
  (reify IFactory
    (-factoryId [this] (byte \e))                            ;;;;;;;;;;;;;;;;;;;;;; e - vector default factory
    (-instanceType [this] nil)
    (-qualified [this t2 opts] this)
    (-sval [this inode opts]
      (default-sval this inode opts))
    (-valueLength [this node opts]
      (default-valueLength this node opts))
    (-deserialize [this node bb]
      (deserialize-sval this node bb))
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
    (-instanceType [this] nil)
    (-qualified [this t2 opts] this)
    (-sval [this inode opts]
      (default-sval this inode opts))
    (-valueLength [this node opts]
      (default-valueLength this node opts))
    (-deserialize [this node bb]
      (let [v (deserialize-sval this node bb)
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
    (-instanceType [this] nil)
    (-qualified [this t2 opts] this)
    (-sval [this inode opts]
      (key-sval this inode opts))
    (-valueLength [this node opts]
      (default-valueLength this node opts))
    (-deserialize [this node bb]
      (let [k (deserialize-sval this node bb)]
        (base/newMapEntry k k)))
    (-writeValue [this node buffer opts]
      (default-write-value this node buffer opts))
    (-valueNode [this node opts] nil)))

(-setDefaultFactory
  set-context
  (factory-for-id
    (byte \q)
    {:factory-registry default-factory-registry}))
