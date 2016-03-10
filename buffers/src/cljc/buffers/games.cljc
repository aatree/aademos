(ns buffers.games
  (:require [aautil.buffer :as buffer]
            [octet.core :as octet]))

#?(:clj
   (set! *warn-on-reflection* true))

(defn bingo []
  (def b (buffer/newBuffer 8))
  (println (buffer/-capacity b))
  (println (buffer/-position b))
  (println (buffer/-limit b))
  (buffer/-position! b 4)
  (buffer/-limit! b 6)
  (println (buffer/-position b))
  (println (buffer/-limit b))
  (buffer/-clear! b)
  (println (buffer/-position b))
  (println (buffer/-limit b))
  (buffer/-position! b 4)
  (buffer/-flip! b)
  (println (buffer/-position b))
  (println (buffer/-limit b))
  (buffer/-position! b 2)
  (buffer/-rewind! b)
  (println (buffer/-position b))
  (println (buffer/-limit b))
  (buffer/-clear! b)
  (buffer/-write! b 42 octet/int32)
  (println (buffer/-position b))
  (buffer/-flip! b)
  (println (buffer/-read! b octet/int32))
  (println (buffer/-position b))
  )
