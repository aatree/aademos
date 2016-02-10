(set-env!
  :dependencies '[[adzerk/boot-cljs                          "1.7.228-1"]
                  [adzerk/boot-cljs-repl                     "0.3.0"]
                  [adzerk/boot-reload                        "0.4.5"]
                  [com.cemerick/piggieback                   "0.2.1"]
                  [weasel                                    "0.7.0"]
                  [org.clojure/tools.nrepl                   "0.2.12"]
                  [hoplon/boot-hoplon                        "0.1.13"]
                  [hoplon/hoplon                             "6.0.0-alpha13"]
                  [org.clojure/clojure                       "1.8.0"]
                  [org.clojure/clojurescript                 "1.7.228"]
                  [tailrecursion/boot-jetty                  "0.1.3"]
                  [aatree/aaworker                           "0.1.1"]]
  :resource-paths #{"assets"}
  :source-paths   #{"src/client" "src/worker"})

(require
  '[adzerk.boot-cljs         :refer [cljs]]
  '[adzerk.boot-cljs-repl    :refer [cljs-repl start-repl]]
  '[adzerk.boot-reload       :refer [reload]]
  '[hoplon.boot-hoplon       :refer [hoplon prerender]]
  '[tailrecursion.boot-jetty :refer [serve]])

(deftask dev
  "Build for local development."
  []
  (comp
    (watch)
    (speak)
    (hoplon)
;    (reload)
;    (cljs-repl)
    (cljs :optimizations :simple)
    (target)
;    (show :fileset true)
    (serve :port 8000 :init-params {"org.eclipse.jetty.servlet.Default.useFileMappedBuffer" "false"})))

(deftask prod
  "Build for production deployment."
  []
  (comp
    (hoplon)
    (cljs :optimizations :advanced)
    (prerender)))
