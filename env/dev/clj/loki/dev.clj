(ns loki.dev
  (:require [cemerick.piggieback :as piggieback]
            [cljs.repl.browser :as repl]
            [figwheel-sidecar.auto-builder :as fig-auto]
            [figwheel-sidecar.core :as fig]
            [clojurescript-build.auto :as auto]))

(defn browser-repl []
  (piggieback/cljs-repl (repl/repl-env)))

(defonce ^:private system
  (atom {:server nil :builder nil}))

(defn- figwheel-server []
  (fig/start-server {:css-dirs ["resources/public/css"]}))

(defn- builds-config []
  [{:source-paths ["src/cljs" "env/dev/cljs"]
    :build-options {:output-to "resources/public/js/app.js"
                    :output-dir "resources/public/js/out"
                    :asset-path "js/out"
                    :optimizations :none
                    :source-map true
                    :cache-analysis true
                    :pretty-print  true}}])

(defn- fig-builder [figwheel-server]
  (fig-auto/autobuild*
   {:builds (builds-config)
    :figwheel-server figwheel-server}))

(defn start-figwheel []
  (let [server (figwheel-server)
        builder (fig-builder server)]
    (reset! system {:server server :builder builder})))

(defn stop-figwheel []
  (let [{:keys [server builder]} @system]
    (when builder
      (auto/stop-autobuild! builder))
    (when server
      (fig/stop-server server)))
  (reset! system {:server nil :builder nil}))
