(set-env!
  :dependencies '[[adzerk/boot-cljs                          "1.7.228-1"]
                  [adzerk/boot-cljs-repl                     "0.3.0"]
                  [com.cemerick/piggieback                   "0.2.1"]
                  [weasel                                    "0.7.0"]
                  [org.clojure/tools.nrepl                   "0.2.12"]
                  [adzerk/boot-reload                        "0.4.4"]
                  [org.clojure/clojure                       "1.8.0"]
                  [org.clojure/clojurescript                 "1.7.228"]
                  [tailrecursion/boot-jetty                  "0.1.3"]
                  [org.clojure/core.async                    "0.2.374"]
                  [servant                                   "0.1.4"]
                  ]
  :source-paths   #{"src/cljs"}
  :resource-paths #{"assets"})

(require
  '[adzerk.boot-cljs         :refer [cljs]]
  '[adzerk.boot-cljs-repl    :refer [cljs-repl start-repl]]
  '[adzerk.boot-reload       :refer [reload]]
  '[tailrecursion.boot-jetty :refer [serve]])

(deftask dev
  "Build for local development."
  []
  (comp
    (watch)
    (speak)
;    (reload)
    (cljs-repl)
    (cljs :optimizations :simple)
;    (show :fileset true)
    (serve :port 9000)))
