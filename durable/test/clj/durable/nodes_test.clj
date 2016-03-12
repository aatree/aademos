(ns durable.nodes-test
  (:require [clojure.test :refer :all]
            [durable.nodes :refer :all])
  (:import (clojure.lang MapEntry RT)))

(def v0 emptyNode)
(pnodev v0 "v0" {})

(def v1 (vector-add v0 1001 0 {}))
(pnodev v1 "v1" {})

(def v01 (vector-add v1 1000 0 {}))
(pnodev v01 "v01" {})

(def v012 (vector-add v01 1002 2 {}))
(pnodev v012 "v012" {})

(pnodev (deln v012 0 {}) "v012 - 0" {})

(pnodev (deln v012 1 {}) "v012 - 1" {})

(pnodev (deln v012 2 {}) "v012 - 2" {})

(def m0 emptyNode)

(def m1 (map-insert m0 (new MapEntry "1" 1001) {:comparator compare}))
(pnodev m1 "m1" {})
(pnodev (map-del m1 "1" {:comparator compare}) "m1 - 1" {})

(def m13 (map-insert m1 (new MapEntry "3" 1003) {:comparator compare}))
(pnodev m13 "m13" {})
(println "m13 level" (.level m13))
(pnodev (map-del m13 "1" {:comparator compare}) "m13 - 1" {})
(pnodev (map-del (map-del m13 "1" {:comparator compare}) "3" {:comparator compare})
        "m13 - -"
        {})
(def m123 (map-insert m13 (new MapEntry "2" 1002) {:comparator compare}))
(pnodev m123 "m123" {})
(pnodev (map-del m123 "1" {:comparator compare}) "m123 - 1" {})
(pnodev (map-del m123 "2" {:comparator compare}) "m123 - 2" {})
(pnodev (map-del m123 "3" {:comparator compare}) "m123 - 3" {})
(pnodev (map-insert m123 (new MapEntry "1" 1001) {:comparator compare}) "m123 + 1" {})
(pnodev (map-insert m123 (new MapEntry "1" 1010) {:comparator compare}) "m123 + 1" {})

(println (new-counted-seq m0 {}))
(println (new-counted-seq m1 {}))
(println (new-counted-seq m13 {}))
(println (new-counted-seq m123 {}))
(println (new-counted-reverse-seq m123 {}))
(println (new-map-key-seq m123 {}))
(println (new-map-key-reverse-seq m123 {}))
(println (new-map-value-seq m123 {}))
(println (new-map-value-reverse-seq m123 {}))

(println "")
(def mi (new-counted-iterator m123 {}))
(println (.hasNext mi))
(println (.next mi))
(println (.hasNext mi))
(println (.next mi))
(println (.hasNext mi))
(println (.next mi))
(println (.hasNext mi))
(println (map-index-of m123 "0" {:comparator compare}))
(println (map-index-of m123 "1" {:comparator compare}))
(println (map-index-of m123 "2" {:comparator compare}))
(println (map-index-of m123 "3" {:comparator compare}))
(println (map-index-of m123 "4" {:comparator compare}))
(println (nth-t2 m123 0 {}))
(println (nth-t2 m123 1 {}))
(println (nth-t2 m123 2 {}))
