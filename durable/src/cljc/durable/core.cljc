(ns durable.core
  #?(:clj
     (:require
       [durable.base :as base]
       [durable.nodes :as nodes]
       [durable.aavec :as aavec])))

#?(:clj
   (set! *warn-on-reflection* true))

(defn default [map key f]
  (if (key map)
    map
    (f map)))

(defn choice [map cond fx fy]
  (if (cond map)
    (fx map)
    (fy map)))

(defn required [map key]
  (if (key map)
    map
    #?(:clj (throw (Exception. (str "missing entry: " key)))
       :cljs (throw (str "missing entry: " key)))))

(defn assoc-default [map key val]
  (if (key map)
    map
    (assoc map key val)))

(defn new-standard-sorted-map [opts]
  (let [c (:comparator opts)]
    (if c
      (sorted-map-by c)
      (sorted-map))))

(defn new-standard-vector [opts] [])

(defn new-standard-sorted-set [opts]
  (let [c (:comparator opts)]
    (if c
      (sorted-set-by c)
      (sorted-set))))

(defn standard-opts
  ([] (standard-opts {}))
  ([opts]
   (-> opts
       (assoc :new-sorted-map new-standard-sorted-map)
       (assoc :new-vector new-standard-vector)
       (assoc :new-sorted-set new-standard-sorted-set))))

#?(:clj
   (do
     (defn addn [vec ndx val]
       (aavec/addNode vec ndx val))

     (defn dropn [vec & args]
       (reduce (fn [v i] (aavec/dropNode v i)) vec args))

     #_(defn new-basic-sorted-map [opts]
       (new AAMap nodes/emptyNode opts))

     (defn new-basic-vector [opts]
       (aavec/create nodes/emptyNode opts))

     #_(defn new-basic-sorted-set [opts]
       (new AASet (new AAMap nodes/emptyNode opts)))

     (defn basic-opts
       ([] (basic-opts {}))
       ([opts]
        (-> opts
            (assoc-default :comparator compare)
            #_(assoc :new-sorted-map new-basic-sorted-map)
            (assoc :new-vector new-basic-vector)
            #_(assoc :new-sorted-set new-basic-sorted-set))))

     (defn new-sorted-map [opts]
       ((:new-sorted-map opts) opts))

     (defn new-vector [opts]
       ((:new-vector opts) opts))

     (defn new-sorted-set [opts]
       ((:new-sorted-set opts) opts))
     ))

#_(do
    (defn load-vector [buffer opts]
      ((:load-vector opts) buffer opts))

    (defn load-sorted-map [buffer opts]
      ((:load-sorted-map opts) buffer opts))

    (defn load-sorted-set [buffer opts]
      ((:load-sorted-set opts) buffer opts))

    (defn byte-length [noded]
      (node-byte-length (get-inode noded) (get-opts noded)))

    (defn put-aa [buffer aa]
      (node-write (get-inode aa) buffer (get-opts aa)))
    )
