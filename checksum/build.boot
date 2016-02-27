(set-env!
  :dependencies '[[adzerk/boot-cljs                          "1.7.228-1"]
                  [hoplon/boot-hoplon                        "0.1.13"]
                  [hoplon/hoplon                             "6.0.0-alpha13"]
                  [org.clojure/clojure                       "1.8.0"]
                  [org.clojure/clojurescript                 "1.7.228"]
                  [pandeiro/boot-http                        "0.7.3"]
                  [adzerk/bootlaces                          "0.1.13"]
                  [adzerk/boot-test                          "1.1.1"]]
  :source-paths   #{"test/clj" "src/clj"})

(require
  '[adzerk.boot-cljs         :refer [cljs]]
  '[adzerk.boot-test         :refer :all]
  '[hoplon.boot-hoplon       :refer [hoplon]]
  '[pandeiro.boot-http       :refer [serve]]
  '[adzerk.bootlaces            :refer :all])

(def +version+ "0.0.1")

(bootlaces! +version+ :dont-modify-paths? true)

(deftask test-it
   "Setup, compile and run the tests."
   []
   (comp
;     (show :fileset true)
     (run-tests)
     ))

#_(deftask dev
  "Build for local development."
  []
  (comp
    (serve :port 8000
           :init 'duracell.strap/jetty-init)
    (watch)
    (speak)
    (hoplon)
    (cljs :optimizations :none)))
