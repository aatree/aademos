(set-env!
  :dependencies '[[adzerk/boot-cljs                          "1.7.228-1"]
                  [adzerk/boot-reload                        "0.4.5"]
                  [hoplon/boot-hoplon                        "0.1.13"]
                  [hoplon/hoplon                             "6.0.0-alpha13"]
                  [org.clojure/clojure                       "1.8.0"]
                  [org.clojure/clojurescript                 "1.8.34"]
                  [pandeiro/boot-http                        "0.7.3"]
                  [aatree/durable-cells                      "0.1.1"]]
  :source-paths   #{"src/client" "src/worker"})

(require
  '[adzerk.boot-cljs         :refer [cljs]]
  '[adzerk.boot-reload       :refer [reload]]
  '[hoplon.boot-hoplon       :refer [hoplon prerender]]
  '[pandeiro.boot-http       :refer [serve]])

(deftask dev
  "Build for local development."
  []
  (comp
    (serve :port 8000
           :init 'duracell.strap/jetty-init)
    (watch)
    (speak)
    (hoplon)
    (reload)
    (cljs :optimizations :none)))
