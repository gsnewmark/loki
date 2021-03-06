(ns loki.app-test
  (:require [cemerick.cljs.test :refer-macros [deftest is]]
            [reagent.core :as reagent]
            [loki.app :as app]))

(def isClient (not (nil? (try (.-document js/window)
                              (catch js/Object e nil)))))

(defn add-test-div [name]
  (let [doc     js/document
        body    (.-body js/document)
        div     (.createElement doc "div")]
    (.appendChild body div)
    div))

(defn with-mounted-component [comp f]
  (when isClient
    (let [div (add-test-div "_testreagent")]
      (let [comp (reagent/render-component comp div #(f comp div))]
        (reagent/unmount-component-at-node div)
        (reagent/flush)
        (.removeChild (.-body js/document) div)))))


(defn found-in [re div]
  (let [res (.-innerHTML div)]
    (if (re-find re res)
      true
      (do (println "Not found: " res)
          false))))


(deftest test-home
  (with-mounted-component (app/page-component)
    (fn [c div]
      (is (found-in #"loki" div)))))
