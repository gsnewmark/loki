(ns ^:figwheel-no-load loki.dev
  (:require [loki.app :as app]
            [figwheel.client :as figwheel :include-macros true]
            [weasel.repl :as weasel]
            [reagent.core :as r]))

(enable-console-print!)

(figwheel/watch-and-reload
  :websocket-url "ws://localhost:3449/figwheel-ws"
  :jsload-callback app/run)

(weasel/connect "ws://localhost:9001" :verbose true)

(app/run)
