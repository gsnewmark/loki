(ns ^:figwheel-no-load loki.dev
  (:require [loki.app :as app]
            [figwheel.client :as figwheel :include-macros true]
            [clojure.browser.repl :as repl]
            [reagent.core :as r]))

(enable-console-print!)

(figwheel/watch-and-reload
  :websocket-url "ws://localhost:3449/figwheel-ws"
  :jsload-callback app/run)

(repl/connect "http://localhost:9000/repl")

(app/run)
