(defproject loki "0.1.0-SNAPSHOT"
  :description "Mashup of Ukraine's map and Twitter users' political sentiment"
  :url "https://github.com/gsnewmark/loki"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :source-paths ["src/clj" "src/cljs"]

  :dependencies [[org.clojure/clojure "1.7.0-beta1"]
                 [org.clojure/clojurescript "0.0-3208"]
                 [cljsjs/react "0.13.1-0"]
                 [reagent "0.5.0"]
                 [re-frame "0.3.1"]
                 [prismatic/schema "0.4.0"]
                 [cljsjs/leaflet "0.7.3-0"]]

  :plugins [[lein-cljsbuild "1.0.6-SNAPSHOT"]]

  :min-lein-version "2.5.0"

  :clean-targets ^{:protect false} ["resources/public/js"]

  :cljsbuild {:builds {:app {:source-paths ["src/cljs"]
                             :compiler {:output-to     "resources/public/js/app.js"
                                        :output-dir    "resources/public/js/out"
                                        :asset-path    "js/out"
                                        :optimizations :none
                                        :pretty-print  true}}}}

  :profiles {:dev {:repl-options {:init-ns loki.repl
                                  :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

                   :dependencies [[figwheel "0.2.7-SNAPSHOT"]
                                  [figwheel-sidecar "0.2.7-SNAPSHOT"]
                                  [com.cemerick/piggieback "0.2.1-SNAPSHOT"]
                                  [org.clojure/tools.nrepl "0.2.10"]
                                  [org.clojure/tools.reader "0.9.1"]
                                  [pjstadig/humane-test-output "0.7.0"]]

                   :source-paths ["env/dev/clj"]
                   :plugins [[com.cemerick/clojurescript.test "0.3.4-SNAPSHOT"]]

                   :injections [(require 'pjstadig.humane-test-output)
                                (pjstadig.humane-test-output/activate!)]

                   :cljsbuild {:builds {:app {:source-paths ["env/dev/cljs"]
                                              :compiler {:main "loki.dev"
                                                         :source-map true}}
                                        :test {:source-paths ["src/cljs"  "test/cljs"]
                                               :compiler {:output-to "target/test.js"
                                                          :optimizations :whitespace
                                                          :pretty-print true
                                                          :preamble ["react/react.js"]}}}
                               :test-commands {"unit" ["phantomjs" :runner
                                                       "test/vendor/es5-shim.js"
                                                       "test/vendor/es5-sham.js"
                                                       "test/vendor/console-polyfill.js"
                                                       "target/test.js"]}}}

             :uberjar {:aot :all
                       :omit-source true
                       :cljsbuild {:jar true
                                   :builds {:app
                                             {:source-paths ["env/prod/cljs"]
                                              :compiler
                                              {:optimizations :advanced
                                               :pretty-print false}}}}}})
