(ns loki.repl
  (:require [loki.dev :as dev]))

(defn start []
  (dev/start-figwheel)
  (dev/browser-repl))

(defn stop []
  (dev/stop-figwheel))
