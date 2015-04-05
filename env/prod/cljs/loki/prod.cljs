(ns loki.prod
  (:require [loki.app :as app]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(app/init!)
