(ns durable.games
  (:require [durable.vec-it :as vec-it]
            [durable.base :as base]
            [durable.nodes :as nodes]))

#?(:clj
   (set! *warn-on-reflection* true))

(defn bingo []
  (def s23 (vec-it/new-counted-seq [1 2 3] 1))
  (println (count s23))
  (println s23)
  (println (first s23))

  (def s3 (next s23))
  (println (count s3))
  (println s3)
  (println (first s3))

  (println (next s3))

  (def me (base/newMapEntry 1 2))
  (println (key me) (val me))

  (def v0 nodes/emptyNode)
  (nodes/pnodev v0 "v0" {})

  (def v1 (base/vector-add v0 1001 0 {}))
  (nodes/pnodev v1 "v1" {})

  (def v01 (base/vector-add v1 1000 0 {}))
  (nodes/pnodev v01 "v01" {})

  (def v012 (base/vector-add v01 1002 2 {}))
  (nodes/pnodev v012 "v012" {})

  (nodes/pnodev (base/deln v012 0 {}) "v012 - 0" {})

  (nodes/pnodev (base/deln v012 1 {}) "v012 - 1" {})

  (nodes/pnodev (base/deln v012 2 {}) "v012 - 2" {})

  (def m0 nodes/emptyNode)

  (def m1 (nodes/map-insert m0 (base/newMapEntry "1" 1001) {:comparator compare}))
  (nodes/pnodev m1 "m1" {})
  (nodes/pnodev (nodes/map-del m1 "1" {:comparator compare}) "m1 - 1" {})

  (def m13 (nodes/map-insert m1 (base/newMapEntry "3" 1003) {:comparator compare}))
  (nodes/pnodev m13 "m13" {})
  (println "m13 level" (base/-getLevel m13 {}))
  (nodes/pnodev (nodes/map-del m13 "1" {:comparator compare}) "m13 - 1" {})
  (nodes/pnodev (nodes/map-del (nodes/map-del m13 "1" {:comparator compare}) "3" {:comparator compare})
                "m13 - -"
                {})
  (def m123 (nodes/map-insert m13 (base/newMapEntry "2" 1002) {:comparator compare}))
  (nodes/pnodev m123 "m123" {})
  (nodes/pnodev (nodes/map-del m123 "1" {:comparator compare}) "m123 - 1" {})
  (nodes/pnodev (nodes/map-del m123 "2" {:comparator compare}) "m123 - 2" {})
  (nodes/pnodev (nodes/map-del m123 "3" {:comparator compare}) "m123 - 3" {})
  (nodes/pnodev (nodes/map-insert m123 (base/newMapEntry "1" 1001) {:comparator compare}) "m123 + 1" {})
  (nodes/pnodev (nodes/map-insert m123 (base/newMapEntry "1" 1010) {:comparator compare}) "m123 + 1" {})

  (println (nodes/new-counted-seq m0 {}))
  (println (nodes/new-counted-seq m1 {}))
  (println (nodes/new-counted-seq m13 {}))
  (println (nodes/new-counted-seq m123 {}))
  (println (nodes/new-counted-reverse-seq m123 {}))
  (println (nodes/new-map-key-seq m123 {}))
  (println (nodes/new-map-key-reverse-seq m123 {}))
  (println (nodes/new-map-value-seq m123 {}))
  (println (nodes/new-map-value-reverse-seq m123 {}))

  (println "")
  (def mi (nodes/new-counted-seq m123 {}))
  (println mi)
  (println (nodes/map-index-of m123 "0" {:comparator compare}))
  (println (nodes/map-index-of m123 "1" {:comparator compare}))
  (println (nodes/map-index-of m123 "2" {:comparator compare}))
  (println (nodes/map-index-of m123 "3" {:comparator compare}))
  (println (nodes/map-index-of m123 "4" {:comparator compare}))
  (println (nodes/nth-t2 m123 0 {}))
  (println (nodes/nth-t2 m123 1 {}))
  (println (nodes/nth-t2 m123 2 {})))
