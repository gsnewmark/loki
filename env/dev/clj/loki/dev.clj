(ns loki.dev
  (:require [cemerick.piggieback :as piggieback]
            [cljs.repl.browser :as repl]
            [leiningen.core.main :as lein]))

(defn browser-repl []
  (piggieback/cljs-repl (repl/repl-env)))

(defn start-figwheel []
  (future
    (print "Starting figwheel.\n")
    (lein/-main ["figwheel"])))
