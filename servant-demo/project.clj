(defproject
  boot-project
  "0.0.0-SNAPSHOT"
  :dependencies
  [[adzerk/boot-cljs "1.7.228-1"]
   [adzerk/boot-cljs-repl "0.3.0"]
   [com.cemerick/piggieback "0.2.1"]
   [weasel "0.7.0"]
   [org.clojure/tools.nrepl "0.2.12"]
   [adzerk/boot-reload "0.4.4"]
   [org.clojure/clojure "1.8.0"]
   [org.clojure/clojurescript "1.7.228"]
   [tailrecursion/boot-jetty "0.1.3"]
   [org.clojure/core.async "0.2.374"]
   [servant "0.1.4"]]
  :source-paths
  ["src/cljs" "assets"])