(ns checksum.bytes)

(defn make-bytes [s]
  (js/Int8Array. s))

(defn set-byte! [a i v]
  (aset a i v))

(defn get-byte [a i]
  (aget a i))

(defn bytes-equal
  ([a1 a2]
   (let [l1 (alength a1)
         l2 (alength a2)]
     (if (= l1 l2)
       (bytes-equal a1 a2 l1)
       false)))
  ([a1 a2 i]
   (if (= i 0)
     true
     (let [i (dec i)]
       (if (not= (aget a1 i) (aget a2 i))
         false
         (recur a1 a2 i))))))

(defn vec-bytes
  ([a]
   (vec-bytes a (list) (alength a)))
  ([a ls i]
   (if (= i 0)
     (vec ls)
     (let [i (dec i)
           ls (conj ls (aget a i))]
       (recur a ls i)))))
