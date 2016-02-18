(set-env!
  :dependencies '[[adzerk/boot-cljs                          "1.7.228-1"]
                  [hoplon/boot-hoplon                        "0.1.13"]
                  [hoplon/hoplon                             "6.0.0-alpha13"]
                  [org.clojure/clojure                       "1.8.0"]
                  [org.clojure/clojurescript                 "1.7.228"]
                  [pandeiro/boot-http                        "0.7.2"]
                  [aatree/durable-cells                      "0.1.0"]]
  :source-paths   #{"src/client" "src/worker"})

(require
  '[adzerk.boot-cljs         :refer [cljs]]
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
    (cljs :optimizations :none)))

(deftask prod
  "Build for production deployment."
  []
  (comp
    (hoplon)
    (cljs :optimizations :advanced)
    (prerender)))
