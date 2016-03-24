(set-env!
  :dependencies '[[adzerk/boot-cljs                          "1.7.228-1"]
                  [com.cemerick/piggieback                   "0.2.2-20150424.212437-1"]
                  [weasel                                    "0.7.0"]
                  [hoplon/boot-hoplon                        "0.1.13"]
                  [hoplon/hoplon                             "6.0.0-alpha13"]
                  [org.clojure/clojure                       "1.8.0"]
                  [org.clojure/clojurescript                 "1.8.34"]
                  [tailrecursion/boot-jetty                  "0.1.3"]
                  [aatree/aaworker                           "0.1.3"]]
  :source-paths   #{"src/client" "src/worker"})

(require
  '[adzerk.boot-cljs         :refer [cljs]]
  '[hoplon.boot-hoplon       :refer [hoplon prerender]]
  '[tailrecursion.boot-jetty :refer [serve]])

(deftask dev
  "Build for local development."
  []
  (comp
    (watch)
    (speak)
    (hoplon)
    (cljs :optimizations :simple)
;    (show :fileset true)
    (serve :port 8000 :init-params {"org.eclipse.jetty.servlet.Default.useFileMappedBuffer" "false"})))
